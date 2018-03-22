/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use android_logger::{self, Filter};
use gl_glue;
use glue::{self, SERVO};
use jni::JNIEnv;
use jni::objects::{GlobalRef, JClass, JObject, JValue, JString};
use jni::sys::{jint, jboolean, jlong, jstring};
use jni::JavaVM;
use log::Level;
use std::sync::Arc;

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_org_mozilla_geckoview_LibServo_version(env: JNIEnv, _class: JClass) -> jstring {
    let v = glue::servo_version();
    let output = env.new_string(format!("Servo Version: {}", v)).expect("Couldn't create java string");
    output.into_inner()
}

/// Needs to be called from the EGL thread
#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_org_mozilla_geckoview_LibServo_init(
    env: JNIEnv, _: JClass,
    url: JString,
    resources_path: JString,
    wakeup_obj: JObject,
    callbacks_obj: JObject,
    width: jint, height: jint) {

    android_logger::init_once(
        Filter::default().with_min_level(Level::Debug)
                         .with_allowed_module_path("servobridge::glue")
                         .with_allowed_module_path("servobridge::api"));

    debug!("api.rs::init");

    let url = env.get_string(url).expect("Couldn't get java string").into();
    let resources_path = env.get_string(resources_path).expect("Couldn't get java string").into();

    let wakeup = WakeupCallback::new(wakeup_obj, &env);
    let callbacks = HostCallbacks::new(callbacks_obj, &env);

    let gl = gl_glue::egl::init();
    glue::init(gl, url, resources_path, wakeup, callbacks, width as u32, height as u32);
    // FIXME: send ServoResult, or throw maybe?
}


/// Needs to be called from the EGL thread
#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_org_mozilla_geckoview_LibServo_resize(
    env: JNIEnv, _: JClass,
    width: jint,
    height: jint) {

    debug!("api.rs::resize {}/{}", width, height);

    let mut res = ServoResult::UnexpectedError;
    SERVO.with(|s| {
        res = s.borrow_mut().as_mut().map(|ref mut s| {
            s.resize(width as u32, height as u32)
        }).unwrap_or(ServoResult::WrongThread)
    });
    res; // FIXME

}

/// This is the Servo heartbeat. This needs to be called
/// everytime wakeup is called.
#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_org_mozilla_geckoview_LibServo_performUpdates(env: JNIEnv, _class: JClass) {
    debug!("api.rs::performUpdates");
    let mut res = ServoResult::UnexpectedError;
    SERVO.with(|s| {
        res = s.borrow_mut().as_mut().map(|ref mut s| {
            s.perform_updates()
        }).unwrap_or(ServoResult::WrongThread)
    });
    res; // FIXME
}

/// Load an URL. This needs to be a valid url.
#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_org_mozilla_geckoview_LibServo_loadUri(env: JNIEnv, _class: JClass, url: JString) {
    debug!("api.rs::loadUri");
    let url = env.get_string(url).expect("Couldn't get java string").into();
    let mut res = ServoResult::UnexpectedError;
    SERVO.with(|s| {
        res = s.borrow_mut().as_mut().map(|ref mut s| {
            s.load_uri(url)
        }).unwrap_or(ServoResult::WrongThread)
    });
    res; // FIXME
}

/// Reload page.
#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_org_mozilla_geckoview_LibServo_reload(env: JNIEnv, _class: JClass) {
    debug!("api.rs::reload");
    let mut res = ServoResult::UnexpectedError;
    SERVO.with(|s| {
        res = s.borrow_mut().as_mut().map(|ref mut s| {
            s.reload()
        }).unwrap_or(ServoResult::WrongThread)
    });
    res; // FIXME
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_org_mozilla_geckoview_LibServo_goBack(env: JNIEnv, _class: JClass) {
    debug!("api.rs::goBack");
    let mut res = ServoResult::UnexpectedError;
    SERVO.with(|s| {
        res = s.borrow_mut().as_mut().map(|ref mut s| {
            s.go_back()
        }).unwrap_or(ServoResult::WrongThread)
    });
    res; // FIXME
}

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_org_mozilla_geckoview_LibServo_goForward(env: JNIEnv, _class: JClass) {
    debug!("api.rs::goForward");
    let mut res = ServoResult::UnexpectedError;
    SERVO.with(|s| {
        res = s.borrow_mut().as_mut().map(|ref mut s| {
            s.go_forward()
        }).unwrap_or(ServoResult::WrongThread)
    });
    res; // FIXME
}


/// Generic result errors
#[repr(C)]
pub enum ServoResult {
    Ok,
    UnexpectedError,
    WrongThread,
    CantReadStr,
    CantParseUrl,    
    NotImplemented,
}

/// Scroll state
#[repr(C)]
pub enum ScrollState {
    Start,
    Move,
    End,
    Canceled,
}

/// Touch state
#[repr(C)]
pub enum TouchState {
    Down,
    Up,
}

pub struct HostCallbacks {
    callbacks: GlobalRef,
    jvm: JavaVM,
}

pub struct WakeupCallback {
    callback: GlobalRef,
    jvm: Arc<JavaVM>,
}

impl WakeupCallback {
    pub fn clone(&self) -> WakeupCallback {
        WakeupCallback {
            callback: self.callback.clone(),
            jvm: self.jvm.clone(),
        }
    }
    pub fn new(jobject: JObject, env: &JNIEnv) -> WakeupCallback {
        let jvm = Arc::new(env.get_java_vm().unwrap());
        WakeupCallback { callback: env.new_global_ref(jobject).unwrap(), jvm }
    }
    /// Will be called from any thread.
    /// Will be called to notify embedder that some events
    /// are available, and that perform_updates need to be called
    pub fn wakeup(&self) {
        debug!("api.rs::wakeup");
        let env = self.jvm.attach_current_thread().unwrap();
        env.call_method(self.callback.as_obj(), "wakeup", "()V", &[]).unwrap();
    }
}

impl HostCallbacks {

    pub fn new(jobject: JObject, env: &JNIEnv) -> HostCallbacks {
        let jvm = env.get_java_vm().unwrap();
        HostCallbacks { callbacks: env.new_global_ref(jobject).unwrap(), jvm }
    }

    /// Will be called from the thread used for the init call
    /// Will be called when the GL buffer has been updated.
    pub fn flush(&self) {
        debug!("api.rs::flush");
        let env = self.jvm.get_env().unwrap();
        env.call_method(self.callbacks.as_obj(), "flush", "()V", &[]).unwrap();
    }

    /// Page starts loading.
    /// "Reload button" becomes "Stop button".
    /// Throbber starts spinning.
    pub fn on_load_started(&self) {
        debug!("api.rs::on_load_started");
        let env = self.jvm.get_env().unwrap();
        env.call_method(self.callbacks.as_obj(), "onLoadStarted", "()V", &[]).unwrap();
    }

    /// Page has loaded.
    /// "Stop button" becomes "Reload button".
    /// Throbber stops spinning.
    pub fn on_load_ended(&self) {
        debug!("api.rs::on_load_ended");
        let env = self.jvm.get_env().unwrap();
        env.call_method(self.callbacks.as_obj(), "onLoadEnded", "()V", &[]).unwrap();
    }

    /// Title changed.
    pub fn on_title_changed(&self, title: String) {
        debug!("api.rs::on_title_changed");
        let env = self.jvm.get_env().unwrap();
        let s = env.new_string(&title).expect("Couldn't create java string").into_inner();
        let s = JValue::from(JObject::from(s));
        env.call_method(self.callbacks.as_obj(), "onTitleChanged", "(Ljava/lang/String;)V", &[s]).unwrap();
    }

    pub fn on_url_changed(&self, url: String) {
        debug!("api.rs::on_url_changed");
        let env = self.jvm.get_env().unwrap();
        let s = env.new_string(&url).expect("Couldn't create java string").into_inner();
        let s = JValue::Object(JObject::from(s));
        env.call_method(self.callbacks.as_obj(), "onUrlChanged", "(Ljava/lang/String;)V", &[s]).unwrap();
    }

    /// Back/forward state changed.
    /// Back/forward buttons need to be disabled/enabled.
    pub fn on_history_changed(&self, can_go_back: bool, can_go_forward: bool) {
        debug!("api.rs::on_history_changed");
        // FIXME
        // let can_go_back = JValue::Bool(can_go_back as jboolean);
        // let can_go_forward = JValue::Bool(can_go_forward as jboolean);
        // let env = self.jvm.get_env().unwrap();
        // env.call_method(self.callbacks.as_obj(), "onHistoryChanged", "(Z;Z)V", &[can_go_back, can_go_forward]).unwrap();
    }
}

#[repr(C)]
#[derive(Debug)]
pub struct Size {
    pub width: u32,
    pub height: u32,
}

#[repr(C)]
#[derive(Debug)]
pub struct Margins {
    pub top: u32,
    pub right: u32,
    pub bottom: u32,
    pub left: u32,
}

#[repr(C)]
#[derive(Debug)]
pub struct Position {
    pub x: i32,
    pub y: i32,
}

// #[no_mangle]
// pub extern "C" fn scroll(dx: i32, dy: i32, x: u32, y: u32, state: ScrollState) -> ServoResult {
//     let mut res = ServoResult::UnexpectedError;
//     SERVO.with(|s| {
//         res = s.borrow_mut().as_mut().map(|ref mut s| {
//             s.scroll(dx, dy, x, y, state)
//         }).unwrap_or(ServoResult::WrongThread)
//     });
//     res
// }

// #[no_mangle]
// pub extern "C" fn click(x: u32, y: u32) -> ServoResult {
//     let mut res = ServoResult::UnexpectedError;
//     SERVO.with(|s| {
//         res = s.borrow_mut().as_mut().map(|ref mut s| {
//             s.click(x, y)
//         }).unwrap_or(ServoResult::WrongThread)
//     });
//     res
// }

// #[no_mangle]
// pub extern "C" fn erase() -> ServoResult {
//     let mut res = ServoResult::UnexpectedError;
//     SERVO.with(|s| {
//         res = s.borrow_mut().as_mut().map(|ref mut s| {
//             s.erase()
//         }).unwrap_or(ServoResult::WrongThread)
//     });
//     res
// }
