use std::collections::HashSet;

pub struct L7SurvivalFirewall {
    pub survival_mode_enabled: bool,
    pub background_stripper_enabled: bool,
    pub whitelisted_apps: HashSet<String>,
    pub suppressed_domains: HashSet<String>,
}

impl L7SurvivalFirewall {
    pub fn new() -> Self {
        let mut suppressed = HashSet::new();
        // Common background telemetry and cloud syncing domains that exhaust low-bandwidth DNS tunnels
        suppressed.insert("play.googleapis.com".to_string());
        suppressed.insert("app-measurement.com".to_string());
        suppressed.insert("crashlytics.com".to_string());
        suppressed.insert("telemetry.sdk.mob.com".to_string());
        suppressed.insert("clients4.google.com".to_string());

        let mut whitelisted = HashSet::new();
        whitelisted.insert("org.telegram.messenger".to_string());
        whitelisted.insert("org.telegram.messenger.web".to_string());
        whitelisted.insert("org.thunderdog.challegram".to_string());

        Self {
            survival_mode_enabled: false,
            background_stripper_enabled: true,
            whitelisted_apps: whitelisted,
            suppressed_domains: suppressed,
        }
    }

    /// Determines whether a domain resolution should be permitted or dropped to preserve bandwidth.
    pub fn should_allow_domain(&self, domain: &str) -> bool {
        if !self.background_stripper_enabled {
            return true;
        }

        let lower = domain.to_lowercase();
        for suppressed in &self.suppressed_domains {
            if lower.ends_with(suppressed) {
                return false;
            }
        }

        true
    }

    /// Evaluates if an application's traffic should be routed through the tunnel in Survival Mode.
    pub fn is_app_whitelisted(&self, package_name: &str) -> bool {
        if !self.survival_mode_enabled {
            return true;
        }

        self.whitelisted_apps.contains(package_name)
    }
}
