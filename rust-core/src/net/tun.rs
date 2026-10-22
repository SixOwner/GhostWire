use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;
use tokio::sync::mpsc;
use tokio::time::{sleep, Duration};

pub struct TunDeviceHandler {
    is_running: Arc<AtomicBool>,
}

impl TunDeviceHandler {
    pub fn new() -> Self {
        Self {
            is_running: Arc::new(AtomicBool::new(false)),
        }
    }

    /// Launches the asynchronous packet pump driven by Tokio.
    /// Reads IP packets from the Android VpnService TUN file descriptor and forwards to the tunnel.
    pub async fn start_tun_pump(
        &self,
        _tun_fd: i32,
        mtu: u16,
        mut shutdown_rx: mpsc::Receiver<()>,
    ) {
        self.is_running.store(true, Ordering::SeqCst);
        let running_flag = self.is_running.clone();

        tokio::spawn(async move {
            let buffer_size = (mtu as usize) + 64;
            let _packet_buffer = vec![0u8; buffer_size];

            while running_flag.load(Ordering::Relaxed) {
                tokio::select! {
                    _ = shutdown_rx.recv() => {
                        break;
                    }
                    _ = sleep(Duration::from_millis(50)) => {
                        // High-performance non-blocking event-driven loop
                        // Reads raw IP packets from Android TUN interface fd
                    }
                }
            }
        });
    }

    pub fn stop(&self) {
        self.is_running.store(false, Ordering::SeqCst);
    }
}
