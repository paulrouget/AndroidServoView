/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use api::*;
use servo::BrowserId;
use servo::Servo;
use servo::compositing::compositor_thread::EventLoopWaker;
use servo::compositing::windowing::{MouseWindowEvent, WindowEvent, WindowMethods};
use servo::euclid::{Point2D, ScaleFactor, Size2D, TypedPoint2D, TypedRect, TypedSize2D, TypedVector2D};
use servo::gl;
use servo::ipc_channel::ipc;
use servo::msg::constellation_msg::{Key, KeyModifiers, TraversalDirection};
use servo::net_traits::net_error_list::NetError;
use servo::script_traits::{LoadData, MouseButton, TouchEventType};
use servo::servo_config::opts;
use servo::servo_config::resource_files::set_resources_path;
use servo::servo_geometry::DeviceIndependentPixel;
use servo::servo_url::ServoUrl;
use servo::style_traits::DevicePixel;
use servo::style_traits::cursor::Cursor;
use servo::webrender_api;
use servo;
use std::cell::RefCell;
use std::ffi::{CStr, CString};
use std::mem;
use std::os::raw::c_char;
use std::rc::Rc;

thread_local! {
    pub static SERVO: RefCell<Option<ServoGlue>> = RefCell::new(None);
}

pub struct ServoGlue {
    servo: Servo<ServoCallbacks>,
    callbacks: Rc<ServoCallbacks>,
    browser_id: BrowserId,
    events: Vec<WindowEvent>,
}

pub fn servo_version() -> *const c_char {
    let servo_version = servo::config::servo_version();
    let text = CString::new(servo_version).unwrap();
    let ptr = text.as_ptr();
    mem::forget(text);
    ptr
}

pub fn init(
    gl: Rc<gl::Gl>,
    url: *const c_char,
    resources_path: *const c_char,
    callbacks: HostCallbacks,
    layout: ViewLayout) -> ServoResult {

    info!("Init: {:?}", layout);

    let resources_path = unsafe { CStr::from_ptr(resources_path) };
    let resources_path = match resources_path.to_str() {
        Ok(path) => path,
        Err(_) => return ServoResult::CantReadStr,
    };

    let url = unsafe { CStr::from_ptr(url) };
    let url = match url.to_str() {
        Ok(url) => url,
        Err(_) => return ServoResult::CantReadStr,
    };

    set_resources_path(Some(resources_path.to_owned()));

    let opts = opts::default_opts();
    opts::set_defaults(opts);

    gl.clear_color(1.0, 1.0, 1.0, 1.0);
    gl.clear(gl::COLOR_BUFFER_BIT);
    gl.finish();

    let callbacks = Rc::new(ServoCallbacks {
        waker: Box::new(RemoteEventLoopWaker(callbacks.wakeup)),
        gl: gl.clone(),
        host_callbacks: callbacks,
        layout: RefCell::new(layout),
    });

    let mut servo = servo::Servo::new(callbacks.clone());

    let url = ServoUrl::parse(url).unwrap();
    let (sender, receiver) = ipc::channel().unwrap();
    servo.handle_events(vec![WindowEvent::NewBrowser(url, sender)]);
    let browser_id = receiver.recv().unwrap();
    servo.handle_events(vec![WindowEvent::SelectBrowser(browser_id)]);

    SERVO.with(|s| {
        *s.borrow_mut() = Some(ServoGlue {
            servo,
            callbacks,
            browser_id,
            events: vec![],
        });
    });

    info!("glue::init::finished");

    ServoResult::Ok
}

impl ServoGlue {

    pub fn perform_updates(&mut self) -> ServoResult {
        info!("perform_updates");
        let events = mem::replace(&mut self.events, Vec::new());
        self.servo.handle_events(events);
        ServoResult::Ok
    }

    pub fn load_url(&mut self, url: *const c_char) -> ServoResult {
        info!("load_url");
        let url = unsafe { CStr::from_ptr(url) };
        url.to_str()
           .map_err(|_| ServoResult::CantReadStr)
           .and_then(|txt| ServoUrl::parse(txt).map_err(|_| ServoResult::CantParseUrl))
           .map(|url| self.servo.handle_events(vec![WindowEvent::LoadUrl(self.browser_id, url)]))
           .map(|_| ServoResult::Ok)
           .unwrap_or_else(|err| err)
    }
    
    pub fn reload(&mut self) -> ServoResult {
        info!("reload");
        self.servo.handle_events(vec![WindowEvent::Reload(self.browser_id)]);
        ServoResult::Ok
    }

    pub fn go_back(&mut self) -> ServoResult {
        info!("go_back");
        let event = WindowEvent::Navigation(self.browser_id, TraversalDirection::Back(1));
        self.servo.handle_events(vec![event]);
        ServoResult::Ok
    }

    pub fn go_forward(&mut self) -> ServoResult {
        info!("go_forward");
        let event = WindowEvent::Navigation(self.browser_id, TraversalDirection::Forward(1));
        self.servo.handle_events(vec![event]);
        ServoResult::Ok
    }

    pub fn resize(&mut self, layout: ViewLayout) -> ServoResult {
        info!("resize");
        *self.callbacks.layout.borrow_mut() = layout;
        self.servo.handle_events(vec![WindowEvent::Resize]);
        ServoResult::Ok
    }

    pub fn scroll(&mut self, dx: i32, dy: i32, x: u32, y: u32, state: ScrollState) -> ServoResult {
        let delta = TypedVector2D::new(dx as f32, dy as f32);
        let scroll_location = webrender_api::ScrollLocation::Delta(delta);
        let phase = match state {
            ScrollState::Start => TouchEventType::Down,
            ScrollState::Move => TouchEventType::Move,
            ScrollState::End => TouchEventType::Up,
            ScrollState::Canceled => TouchEventType::Cancel,
        };
        let event = WindowEvent::Scroll(scroll_location, TypedPoint2D::new(x as i32, y as i32), phase);
        self.servo.handle_events(vec![event]);
        ServoResult::Ok
    }

    pub fn click(&mut self, x: u32, y: u32) -> ServoResult {
        let mouse_event= MouseWindowEvent::Click(MouseButton::Left, TypedPoint2D::new(x as f32, y as f32));
        let event = WindowEvent::MouseWindowEventClass(mouse_event);
        self.servo.handle_events(vec![event]);
        ServoResult::Ok
    }

    pub fn erase(&mut self) -> ServoResult {
        info!("erase");
        let url = ServoUrl::parse("about:blank").unwrap();
        let (sender, receiver) = ipc::channel().unwrap();
        self.servo.handle_events(vec![WindowEvent::NewBrowser(url, sender)]);
        let id = receiver.recv().unwrap();
        let event = WindowEvent::SelectBrowser(id);
        self.servo.handle_events(vec![event]);
        let last_id = self.browser_id;
        self.browser_id = id;
        let event = WindowEvent::CloseBrowser(last_id);
        self.servo.handle_events(vec![event]);
        ServoResult::Ok
    }

}


pub struct RemoteEventLoopWaker(extern fn());

impl EventLoopWaker for RemoteEventLoopWaker {
    fn clone(&self) -> Box<EventLoopWaker + Send> {
        Box::new(RemoteEventLoopWaker(self.0))
    }
    fn wake(&self) {
        (self.0)();
    }
}

struct ServoCallbacks {
    waker: Box<EventLoopWaker>,
    gl: Rc<gl::Gl>,
    host_callbacks: HostCallbacks,
    layout: RefCell<ViewLayout>,
}

impl WindowMethods for ServoCallbacks {
    fn prepare_for_composite(&self, _width: usize, _height: usize) -> bool {
        info!("WindowMethods::prepare_for_composite");
        true
    }

    fn present(&self) {
        info!("WindowMethods::present");
        (self.host_callbacks.flush)();
    }

    fn supports_clipboard(&self) -> bool {
        info!("WindowMethods::supports_clipboard");
        false
    }

    fn create_event_loop_waker(&self) -> Box<EventLoopWaker> {
        info!("WindowMethods::create_event_loop_waker");
        self.waker.clone()
    }

    fn gl(&self) -> Rc<gl::Gl> {
        info!("WindowMethods::gl");
        self.gl.clone()
    }

    fn hidpi_factor(&self) -> ScaleFactor<f32, DeviceIndependentPixel, DevicePixel> {
        info!("WindowMethods::hidpi_factor");
        ScaleFactor::new(self.layout.borrow().hidpi_factor)
    }

    fn framebuffer_size(&self) -> TypedSize2D<u32, DevicePixel> {
        info!("WindowMethods::framebuffer_size");
        TypedSize2D::new(self.layout.borrow().view_size.width, self.layout.borrow().view_size.height)
    }

    fn window_rect(&self) -> TypedRect<u32, DevicePixel> {
        info!("WindowMethods::window_rect");
        TypedRect::new(TypedPoint2D::new(0, 0), self.framebuffer_size())
    }

    fn size(&self) -> TypedSize2D<f32, DeviceIndependentPixel> {
        info!("WindowMethods::size");
        let l = self.layout.borrow();
        let width = l.view_size.width as f32;
        let height = l.view_size.height as f32;
        let factor = l.hidpi_factor;
        TypedSize2D::new(width / factor, height / factor)
    }

    fn client_window(&self, _id: BrowserId) -> (Size2D<u32>, Point2D<i32>) {
        info!("WindowMethods::client_window");
        let l = self.layout.borrow();
        let factor = l.hidpi_factor;
        let width: u32 = (l.view_size.width as f32 / factor) as u32;
        let height: u32 = (l.view_size.height as f32 / factor) as u32;
        (Size2D::new(width, height), Point2D::new(0, 0))
    }

    fn load_start(&self, _id: BrowserId) {
        info!("WindowMethods::load_start");
        (self.host_callbacks.on_load_started)();
    }

    fn load_end(&self, _id: BrowserId) {
        info!("WindowMethods::load_end");
        (self.host_callbacks.on_load_ended)();
    }

    fn history_changed(&self, _id: BrowserId, entries: Vec<LoadData>, current: usize) {
        info!("WindowMethods::history_changed");
        let can_go_back = current > 0;
        let can_go_forward = current < entries.len() - 1;
        (self.host_callbacks.on_history_changed)(can_go_back, can_go_forward);
        let url = entries[current].url.to_string();
        let url = CString::new(url).unwrap();
        let url_ptr = url.as_ptr();
        mem::forget(url);
        // FIXME: when to free url_ptr?
        (self.host_callbacks.on_url_changed)(url_ptr);
    }

    fn screen_size(&self, id: BrowserId) -> Size2D<u32> {
        info!("WindowMethods::screen_size");
        self.client_window(id).0
    }

    fn screen_avail_size(&self, id: BrowserId) -> Size2D<u32> {
        info!("WindowMethods::screen_avail_size");
        self.screen_size(id)
    }


    fn allow_navigation(&self, _id: BrowserId, _url: ServoUrl, chan: ipc::IpcSender<bool>) { chan.send(true).ok(); }
    fn set_inner_size(&self, _id: BrowserId, _size: Size2D<u32>) {}
    fn set_position(&self, _id: BrowserId, _point: Point2D<i32>) {}
    fn set_fullscreen_state(&self, _id: BrowserId, _state: bool) {}
    fn set_page_title(&self, _id: BrowserId, _title: Option<String>) {}
    fn status(&self, _id: BrowserId, _status: Option<String>) {}
    fn load_error(&self, _id: BrowserId, _: NetError, _url: String) {}
    fn head_parsed(&self, _id: BrowserId) {}
    fn set_cursor(&self, _cursor: Cursor) { }
    fn set_favicon(&self, _id: BrowserId, _url: ServoUrl) {}
    fn handle_key(&self, _id: Option<BrowserId>, _ch: Option<char>, _key: Key, _mods: KeyModifiers) { }
}
