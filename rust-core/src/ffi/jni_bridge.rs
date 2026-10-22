use jni::objects::{JClass, JObject, JString};
use jni::sys::{jboolean, jint, jstring, JNI_TRUE};
use jni::JNIEnv;

use crate::crypto::obfuscation::CloakedNodeCredentials;
use crate::net::mtu::AdaptiveMtuEngine;
use crate::protocols::slipnet::{RuntimeProfile, UniversalImporter};
use crate::scanner::latency::LatencyEngine;
use crate::scanner::probe::ResolverProbe;
use crate::scanner::resolver_db::ResolverNode;

use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;
use tokio::runtime::Runtime;

lazy_static::lazy_static! {
    static ref TOKIO_RT: Runtime = Runtime::new().expect("Failed to initialize Tokio runtime");
    static ref TUNNEL_ACTIVE: Arc<AtomicBool> = Arc::new(AtomicBool::new(false));
}

#[no_mangle]
pub extern "system" fn Java_net_irazad_ghostwire_vpn_NativeBridge_initNativeCore(
    _env: JNIEnv,
    _class: JClass,
    _context: JObject,
) -> jboolean {
    JNI_TRUE
}

#[no_mangle]
pub extern "system" fn Java_net_irazad_ghostwire_vpn_NativeBridge_getCloakedFactoryProfiles(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let creds = CloakedNodeCredentials::load_primary_node();

    // Node Alpha (Blackout Ready) - WireReality over 443 with SNI masquerading
    let profile_reality = RuntimeProfile {
        id: "builtin_alpha_reality".to_string(),
        name: "Ghost Core - Node Alpha [Blackout Ready]".to_string(),
        protocol: "WireReality".to_string(),
        server_address: creds.server_ip.clone(),
        server_port: 443,
        uuid: Some(creds.reality_uuid),
        public_key: Some(creds.reality_pubkey),
        short_id: Some(creds.reality_short_id),
        sni: Some(creds.reality_sni),
        tunnel_domain: None,
        resolver_ip: None,
        mtu: 1400,
        noise_injection: true,
        is_builtin: true,
        ping_ms: 42,
    };

    // Node Beta (National Emergency) - GhostDNS Core over UDP 53 via domestic resolvers
    let profile_dns = RuntimeProfile {
        id: "builtin_beta_dnstt".to_string(),
        name: "Ghost DNS - Node Beta [Emergency Tunnel]".to_string(),
        protocol: "GhostDNS Core".to_string(),
        server_address: creds.server_ip,
        server_port: 53,
        uuid: None,
        public_key: Some(creds.dnstt_pubkey),
        short_id: None,
        sni: None,
        tunnel_domain: Some(creds.dnstt_domain),
        resolver_ip: Some("10.201.34.14".to_string()),
        mtu: 1232,
        noise_injection: true,
        is_builtin: true,
        ping_ms: 38,
    };

    let factory_list = vec![profile_reality, profile_dns];
    let json_output = serde_json::to_string(&factory_list).unwrap_or_else(|_| "[]".to_string());

    env.new_string(json_output)
        .expect("Failed to create JString")
        .into_raw()
}

#[no_mangle]
pub extern "system" fn Java_net_irazad_ghostwire_vpn_NativeBridge_parseConfigurationUri(
    mut env: JNIEnv,
    _class: JClass,
    uri_jstring: JString,
) -> jstring {
    let input: String = match env.get_string(&uri_jstring) {
        Ok(s) => s.into(),
        Err(_) => {
            return env
                .new_string("{\"error\":\"Invalid string\"}")
                .unwrap()
                .into_raw()
        }
    };

    let result = match UniversalImporter::parse(&input) {
        Ok(profile) => serde_json::to_string(&profile).unwrap_or_default(),
        Err(err) => format!("{{\"error\":\"{}\"}}", err),
    };

    env.new_string(result)
        .expect("Failed to create JString")
        .into_raw()
}

#[no_mangle]
pub extern "system" fn Java_net_irazad_ghostwire_vpn_NativeBridge_measureProfileLatency(
    mut env: JNIEnv,
    _class: JClass,
    profile_json_jstring: JString,
) -> jint {
    let json_str: String = match env.get_string(&profile_json_jstring) {
        Ok(s) => s.into(),
        Err(_) => return -1,
    };

    let profile: RuntimeProfile = match serde_json::from_str(&json_str) {
        Ok(p) => p,
        Err(_) => return -1,
    };

    TOKIO_RT.block_on(async { LatencyEngine::measure_profile_latency(&profile).await })
}

#[no_mangle]
pub extern "system" fn Java_net_irazad_ghostwire_vpn_NativeBridge_measureAllProfiles(
    mut env: JNIEnv,
    _class: JClass,
    profiles_json_jstring: JString,
) -> jstring {
    let json_str: String = match env.get_string(&profiles_json_jstring) {
        Ok(s) => s.into(),
        Err(_) => return env.new_string("[]").unwrap().into_raw(),
    };

    let profiles: Vec<RuntimeProfile> = serde_json::from_str(&json_str).unwrap_or_default();

    let sorted = TOKIO_RT.block_on(async { LatencyEngine::test_all_servers(profiles).await });
    let output_json = serde_json::to_string(&sorted).unwrap_or_else(|_| "[]".to_string());

    env.new_string(output_json)
        .expect("Failed to create JString")
        .into_raw()
}

#[no_mangle]
pub extern "system" fn Java_net_irazad_ghostwire_vpn_NativeBridge_executeRadarScan(
    mut env: JNIEnv,
    _class: JClass,
    resolvers_json_jstring: JString,
) -> jstring {
    let json_str: String = match env.get_string(&resolvers_json_jstring) {
        Ok(s) => s.into(),
        Err(_) => return env.new_string("[]").unwrap().into_raw(),
    };

    let nodes: Vec<ResolverNode> = serde_json::from_str(&json_str).unwrap_or_default();

    let scanned = TOKIO_RT.block_on(async {
        let mut tasks = Vec::new();
        for node in nodes {
            tasks.push(tokio::spawn(async move {
                ResolverProbe::probe_resolver(node).await
            }));
        }

        let mut results = Vec::new();
        for task in tasks {
            if let Ok(res) = task.await {
                results.push(res);
            }
        }
        results
    });

    let output_json = serde_json::to_string(&scanned).unwrap_or_else(|_| "[]".to_string());
    env.new_string(output_json)
        .expect("Failed to create JString")
        .into_raw()
}

#[no_mangle]
pub extern "system" fn Java_net_irazad_ghostwire_vpn_NativeBridge_discoverOptimalMtu(
    mut env: JNIEnv,
    _class: JClass,
    target_host_jstring: JString,
) -> jint {
    let host: String = match env.get_string(&target_host_jstring) {
        Ok(s) => s.into(),
        Err(_) => return 1280,
    };

    let target_addr = match format!("{}:53", host).parse() {
        Ok(a) => a,
        Err(_) => return 1280,
    };

    TOKIO_RT.block_on(async { AdaptiveMtuEngine::detect_optimal_pmtu(target_addr).await as jint })
}

#[no_mangle]
pub extern "system" fn Java_net_irazad_ghostwire_vpn_NativeBridge_startTunnel(
    _env: JNIEnv,
    _class: JClass,
    _tun_fd: jint,
    _profile_json: JString,
) -> jboolean {
    TUNNEL_ACTIVE.store(true, Ordering::SeqCst);
    JNI_TRUE
}

#[no_mangle]
pub extern "system" fn Java_net_irazad_ghostwire_vpn_NativeBridge_stopTunnel(
    _env: JNIEnv,
    _class: JClass,
) {
    TUNNEL_ACTIVE.store(false, Ordering::SeqCst);
}
