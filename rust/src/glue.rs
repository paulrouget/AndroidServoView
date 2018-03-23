/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use api::*;
use servo::BrowserId;
use servo::Servo;
use servo::compositing::compositor_thread::{EmbedderMsg, EventLoopWaker};
use servo::compositing::windowing::{EmbedderCoordinates, AnimationState, MouseWindowEvent, WindowEvent, WindowMethods};
use servo::euclid::{Length, TypedScale, TypedPoint2D, TypedSize2D, TypedVector2D};
use servo::gl;
use servo::ipc_channel::ipc;
use servo::msg::constellation_msg::TraversalDirection;
use servo::script_traits::{MouseButton, TouchEventType};
use servo::servo_config::opts;
use servo::servo_config::resource_files::set_resources_path;
use servo::servo_url::ServoUrl;
use servo::style_traits::DevicePixel;
use servo::webrender_api;
use servo;
use std::cell::{Cell, RefCell};
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
    current_url: Option<ServoUrl>,
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
    servo.handle_events(vec![WindowEvent::NewBrowser(url.clone(), sender)]);
    let browser_id = receiver.recv().unwrap();
    servo.handle_events(vec![WindowEvent::SelectBrowser(browser_id)]);

    SERVO.with(|s| {
        *s.borrow_mut() = Some(ServoGlue {
            servo,
            callbacks,
            browser_id,
            events: vec![],
            current_url: Some(url),
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
        info!("load_uri: {}", url);
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
        self.events.push(event);
        ServoResult::Ok
    }

    pub fn click(&mut self, x: u32, y: u32) -> ServoResult {
        let mouse_event= MouseWindowEvent::Click(MouseButton::Left, TypedPoint2D::new(x as f32, y as f32));
        let event = WindowEvent::MouseWindowEventClass(mouse_event);
        self.servo.handle_events(vec![event]);
        ServoResult::Ok
    }

    pub fn handle_servo_events(&mut self) {
        for event in self.servo.get_events() {
            match event {
                EmbedderMsg::ChangePageTitle(_browser_id, title) => {
                    let fallback_title: String = if let Some(ref current_url) = self.current_url {
                        current_url.to_string()
                    } else {
                        String::from("Untitled")
                    };
                    let title = match title {
                        Some(ref title) if title.len() > 0 => &**title,
                        _ => &fallback_title,
                    };
                    let title = format!("{} - Servo", title);
                    self.callbacks.host_callbacks.on_title_changed(title);
                }
                EmbedderMsg::AllowNavigation(_browser_id, _url, response_chan) => {
                    if let Err(e) = response_chan.send(true) {
                        warn!("Failed to send allow_navigation() response: {}", e);
                    };
                }
                EmbedderMsg::HistoryChanged(_browser_id, entries, current) => {
                    let can_go_back = current > 0;
                    let can_go_forward = current < entries.len() - 1;
                    self.callbacks.host_callbacks.on_history_changed(can_go_back, can_go_forward);
                    self.callbacks.host_callbacks.on_url_changed(entries[current].url.clone().to_string());
                    self.current_url = Some(entries[current].url.clone());
                }
                EmbedderMsg::LoadStart(_browser_id) => {
                    self.callbacks.host_callbacks.on_load_started();
                }
                EmbedderMsg::LoadComplete(_browser_id) => {
                    self.callbacks.host_callbacks.on_load_ended();
                }
                EmbedderMsg::Status(..) |
                EmbedderMsg::MoveTo(..) |
                EmbedderMsg::ResizeTo(..) |
                EmbedderMsg::KeyEvent(..) |
                EmbedderMsg::SetCursor(..) |
                EmbedderMsg::NewFavicon(..) |
                EmbedderMsg::HeadParsed(..) |
                EmbedderMsg::SetFullscreenState(..) |
                EmbedderMsg::Shutdown |
                EmbedderMsg::Panic(..) => {
                }
            }
        }
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

    fn set_animation_state(&self, _state: AnimationState) {
        // FIXME: use Choreographer
    }

    fn get_coordinates(&self) -> EmbedderCoordinates {
        let size = TypedSize2D::new(self.width.get(), self.height.get());
        EmbedderCoordinates {
            viewport: webrender_api::DeviceUintRect::new(TypedPoint2D::zero(), size),
            framebuffer: size,
            window: (size, TypedPoint2D::new(0,0)),
            screen: size,
            // FIXME: Glutin doesn't have API for available size. Fallback to screen size
            screen_avail: size,
            hidpi_factor: TypedScale::new(2.0),
        }
    }
}
