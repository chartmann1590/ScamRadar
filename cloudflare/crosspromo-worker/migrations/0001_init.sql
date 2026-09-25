-- Migration: 0001_init.sql
-- Hartmann Studios Dynamic Cross-Promotion Platform (Cloudflare D1 schema)

CREATE TABLE IF NOT EXISTS apps (
    package_name TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    icon_url TEXT,
    store_url TEXT NOT NULL,
    short_description TEXT,
    rating REAL,
    review_count INTEGER,
    install_text TEXT,
    estimated_minimum_installs INTEGER DEFAULT 0,
    category TEXT,
    price_text TEXT DEFAULT 'Free',
    is_free INTEGER DEFAULT 1,
    developer TEXT DEFAULT 'Hartmann Studios',
    first_discovered_at TEXT NOT NULL,
    last_seen_at TEXT NOT NULL,
    last_metadata_refresh TEXT NOT NULL,
    enabled_for_promotion INTEGER DEFAULT 1,
    promotion_multiplier REAL DEFAULT 1.0,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS catalog_refreshes (
    refresh_id TEXT PRIMARY KEY NOT NULL,
    timestamp TEXT NOT NULL,
    status TEXT NOT NULL, -- 'success', 'suspicious_drop', 'error'
    source TEXT NOT NULL, -- 'google_play_scrape', 'authenticated_api'
    discovered_count INTEGER NOT NULL,
    accepted_count INTEGER NOT NULL,
    rejected_count INTEGER NOT NULL,
    rejection_reason TEXT,
    metadata_failures INTEGER DEFAULT 0,
    duration_ms INTEGER DEFAULT 0,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS promo_events (
    event_id TEXT PRIMARY KEY NOT NULL,
    timestamp TEXT NOT NULL,
    event_type TEXT NOT NULL, -- 'promo_impression', 'promo_click', 'crosspromo_install'
    source_package TEXT NOT NULL,
    target_package TEXT NOT NULL,
    placement TEXT NOT NULL,
    rank_position INTEGER,
    selection_type TEXT, -- 'popular', 'random', 'new_app_boost'
    session_id TEXT,
    recommendation_request_id TEXT,
    sdk_version TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_promo_events_target ON promo_events(target_package, event_type);
CREATE INDEX IF NOT EXISTS idx_promo_events_source ON promo_events(source_package, placement);
CREATE INDEX IF NOT EXISTS idx_promo_events_timestamp ON promo_events(timestamp);

CREATE TABLE IF NOT EXISTS promo_stats (
    stat_key TEXT PRIMARY KEY NOT NULL, -- composite key e.g. target_package:placement or target_package
    target_package TEXT NOT NULL,
    placement TEXT,
    source_package TEXT,
    impressions INTEGER DEFAULT 0,
    clicks INTEGER DEFAULT 0,
    installs INTEGER DEFAULT 0,
    ctr REAL GENERATED ALWAYS AS (CASE WHEN impressions > 0 THEN CAST(clicks AS REAL) / CAST(impressions AS REAL) ELSE 0.0 END) STORED,
    last_updated TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS config (
    config_key TEXT PRIMARY KEY NOT NULL,
    config_value TEXT NOT NULL,
    description TEXT,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_config (
    package_name TEXT PRIMARY KEY NOT NULL,
    enabled INTEGER DEFAULT 1,
    max_cards INTEGER DEFAULT 3,
    allowed_placements TEXT, -- JSON array e.g. '["settings", "home"]'
    blocked_targets TEXT, -- JSON array of packages not to show in this app
    promotion_multiplier REAL DEFAULT 1.0,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- Seed default global config
INSERT OR IGNORE INTO config (config_key, config_value, description) VALUES
('enabled', 'true', 'Global kill switch for cross-promotion'),
('popularWeight', '0.65', 'Ratio of popular-weighted discovery vs random exploration'),
('randomWeight', '0.35', 'Ratio of exploration discovery'),
('newAppBoostDays', '14', 'Days new apps receive exploration boost'),
('defaultLimit', '3', 'Default number of recommended apps returned'),
('maxLimit', '6', 'Maximum number of recommended apps returned'),
('recommendationTtlHours', '6', 'Cache duration for recommendations on client');
