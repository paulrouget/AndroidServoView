/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use api::*;
use servo::BrowserId;
use servo::Servo;
use servo::compositing::compositor_thread::EventLoopWaker;
use servo::compositing::windowing::{MouseWindowEvent, WindowEvent, WindowMethods};
use servo::euclid::{Length, Point2D, ScaleFactor, Size2D, TypedPoint2D, TypedRect, TypedSize2D, TypedVector2D};
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
use servo::style_traits::cursor::CursorKind;
use servo::webrender_api;
use servo;
use std::cell::{Cell, RefCell};
use std::ffi::{CStr, CString};
use std::mem;
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

pub fn servo_version() -> String {
    servo::config::servo_version()
}

pub fn init(
    gl: Rc<gl::Gl>,
    url: String,
    resources_path: String,
    wakeup: WakeupCallback,
    callbacks: HostCallbacks,
    width: u32, height: u32) -> ServoResult {

    info!("Init");

    set_resources_path(Some(resources_path));

    let opts = opts::default_opts();
    opts::set_defaults(opts);

    gl.clear_color(1.0, 1.0, 1.0, 1.0);
    gl.clear(gl::COLOR_BUFFER_BIT);
    gl.finish();

    let callbacks = Rc::new(ServoCallbacks {
        waker: Box::new(RemoteEventLoopWaker(wakeup)),
        gl: gl.clone(),
        host_callbacks: callbacks,
        width: Cell::new(width),
        height: Cell::new(height),
    });

    let mut servo = servo::Servo::new(callbacks.clone());

    let url = ServoUrl::parse(&url).unwrap();
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
        debug!("perform_updates");
        let events = mem::replace(&mut self.events, Vec::new());
        self.servo.handle_events(events);
        ServoResult::Ok
    }

    pub fn load_uri(&mut self, url: String) -> ServoResult {
        info!("load_uri");
        ServoUrl::parse(&url)
           .map_err(|_| ServoResult::CantParseUrl)
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

    pub fn resize(&mut self, width: u32, height: u32) -> ServoResult {
        info!("resize");
        self.callbacks.width.set(width);
        self.callbacks.height.set(height);
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
        let url = ServoUrl::parse("https://servo.org").unwrap();
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


pub struct RemoteEventLoopWaker(WakeupCallback);

impl EventLoopWaker for RemoteEventLoopWaker {
    fn clone(&self) -> Box<EventLoopWaker + Send> {
        Box::new(RemoteEventLoopWaker(self.0.clone()))
    }
    fn wake(&self) {
        self.0.wakeup();
    }
}

struct ServoCallbacks {
    waker: Box<EventLoopWaker>,
    gl: Rc<gl::Gl>,
    host_callbacks: HostCallbacks,
    width: Cell<u32>,
    height: Cell<u32>,
}

impl WindowMethods for ServoCallbacks {
    fn prepare_for_composite(&self, _width: Length<u32, DevicePixel>, _height: Length<u32, DevicePixel>) -> bool {
        info!("WindowMethods::prepare_for_composite");
        true
    }

    fn present(&self) {
        info!("WindowMethods::present");
        self.host_callbacks.flush();
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
        // FIXME
        ScaleFactor::new(2.0)
    }

    fn framebuffer_size(&self) -> TypedSize2D<u32, DevicePixel> {
        info!("WindowMethods::framebuffer_size");
        TypedSize2D::new(self.width.get(), self.height.get())
    }

    fn window_rect(&self) -> TypedRect<u32, DevicePixel> {
        info!("WindowMethods::window_rect");
        TypedRect::new(TypedPoint2D::new(0, 0), self.framebuffer_size())
    }

    fn client_window(&self, _id: BrowserId) -> (TypedSize2D<u32, DevicePixel>, TypedPoint2D<i32, DevicePixel>) {
        info!("WindowMethods::client_window");
        (self.framebuffer_size(), TypedPoint2D::new(0, 0))
    }

    fn load_start(&self, _id: BrowserId) {
        info!("WindowMethods::load_start");
        self.host_callbacks.on_load_started();
    }

    fn load_end(&self, _id: BrowserId) {
        info!("WindowMethods::load_end");
        self.host_callbacks.on_load_ended();
    }

    fn history_changed(&self, _id: BrowserId, entries: Vec<LoadData>, current: usize) {
        info!("WindowMethods::history_changed");
        let can_go_back = current > 0;
        let can_go_forward = current < entries.len() - 1;
        self.host_callbacks.on_history_changed(can_go_back, can_go_forward);
        let url = entries[current].url.to_string();
        self.host_callbacks.on_url_changed(url);
    }

    fn screen_size(&self, id: BrowserId) -> TypedSize2D<u32, DevicePixel> {
        info!("WindowMethods::screen_size");
        self.client_window(id).0
    }

    fn screen_avail_size(&self, id: BrowserId) -> TypedSize2D<u32, DevicePixel> {
        info!("WindowMethods::screen_avail_size");
        self.screen_size(id)
    }

    fn set_page_title(&self, _id: BrowserId, title: Option<String>) {
        self.host_callbacks.on_title_changed(title.unwrap_or("No Title".to_string()));
    }

    fn handle_panic(&self, _: BrowserId, reason: String, _backtrace: Option<String>) {
        debug!("PANIC!!! {}", reason);
    }

    fn allow_navigation(&self, _id: BrowserId, _url: ServoUrl, chan: ipc::IpcSender<bool>) { chan.send(true).ok(); }
    fn set_inner_size(&self, _id: BrowserId, _size: TypedSize2D<u32, DevicePixel>) {}
    fn set_position(&self, _id: BrowserId, _point: TypedPoint2D<i32, DevicePixel>) {}
    fn set_fullscreen_state(&self, _id: BrowserId, _state: bool) {}
    fn status(&self, _id: BrowserId, _status: Option<String>) {}
    fn load_error(&self, _id: BrowserId, _: NetError, _url: String) {}
    fn head_parsed(&self, _id: BrowserId) {}
    fn set_cursor(&self, _cursor: CursorKind) { }
    fn set_favicon(&self, _id: BrowserId, _url: ServoUrl) {}
    fn handle_key(&self, _id: Option<BrowserId>, _ch: Option<char>, _key: Key, _mods: KeyModifiers) { }
}
