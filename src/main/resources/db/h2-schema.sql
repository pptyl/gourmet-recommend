-- =====================================================================
-- 美食推荐系统 - H2 建表脚本（dev 环境自动执行）
-- =====================================================================
drop table if exists browse_history;
create table browse_history (
    id bigint NOT NULL auto_increment,
    user_id bigint NOT NULL,
    merchant_id bigint NOT NULL,
    dish_id bigint DEFAULT NULL,
    create_time timestamp DEFAULT NULL,
    PRIMARY KEY (id)
);
create index idx_browse_user on browse_history (user_id,create_time);

drop table if exists coupon;
create table coupon (
    id bigint NOT NULL auto_increment,
    merchant_id bigint NOT NULL,
    title varchar(100) NOT NULL,
    description varchar(500) DEFAULT NULL,
    threshold_amount decimal(10,2) NOT NULL,
    discount_amount decimal(10,2) NOT NULL,
    total_stock int NOT NULL,
    remain_stock int NOT NULL,
    grab_begin_time timestamp DEFAULT NULL,
    grab_end_time timestamp DEFAULT NULL,
    valid_start_time timestamp DEFAULT NULL,
    valid_end_time timestamp DEFAULT NULL,
    per_limit int DEFAULT '1',
    status tinyint DEFAULT '1',
    create_time timestamp DEFAULT NULL,
    update_time timestamp DEFAULT NULL,
    deleted tinyint DEFAULT '0',
    PRIMARY KEY (id)
);
create index idx_coupon_merchant on coupon (merchant_id);

drop table if exists dish;
create table dish (
    id bigint NOT NULL auto_increment,
    merchant_id bigint NOT NULL,
    name varchar(100) NOT NULL,
    price decimal(10,2) NOT NULL,
    intro varchar(1000) DEFAULT NULL,
    image varchar(255) DEFAULT NULL,
    status tinyint DEFAULT '1',
    sales int DEFAULT '0',
    create_time timestamp DEFAULT NULL,
    update_time timestamp DEFAULT NULL,
    deleted tinyint DEFAULT '0',
    PRIMARY KEY (id)
);
create index idx_dish_merchant on dish (merchant_id);

drop table if exists favorite;
create table favorite (
    id bigint NOT NULL auto_increment,
    user_id bigint NOT NULL,
    merchant_id bigint NOT NULL,
    create_time timestamp DEFAULT NULL,
    PRIMARY KEY (id)
);
create unique index uk_favorite_user_merchant on favorite (user_id,merchant_id);

drop table if exists merchant;
create table merchant (
    id bigint NOT NULL auto_increment,
    name varchar(100) NOT NULL,
    phone varchar(20) NOT NULL,
    password varchar(100) NOT NULL,
    category varchar(50) DEFAULT NULL,
    area varchar(50) DEFAULT NULL,
    address varchar(255) DEFAULT NULL,
    intro varchar(1000) DEFAULT NULL,
    cover_image varchar(255) DEFAULT NULL,
    album varchar(2000) DEFAULT NULL,
    business_hours varchar(100) DEFAULT NULL,
    avg_price decimal(10,2) DEFAULT '0.00',
    score decimal(3,1) DEFAULT '5.0',
    score_count int DEFAULT '0',
    favorite_count int DEFAULT '0',
    review_count int DEFAULT '0',
    view_count int DEFAULT '0',
    status tinyint DEFAULT '1',
    audit_status tinyint DEFAULT '0',
    audit_remark varchar(255) DEFAULT NULL,
    create_time timestamp DEFAULT NULL,
    update_time timestamp DEFAULT NULL,
    deleted tinyint DEFAULT '0',
    PRIMARY KEY (id),
    UNIQUE KEY uk_merchant_phone (phone),
    KEY idx_merchant_category (category),
    KEY idx_merchant_area (area)
);
create index idx_merchant_audit on merchant (audit_status);

drop table if exists message;
create table message (
    id bigint NOT NULL auto_increment,
    receiver_id bigint NOT NULL,
    receiver_type tinyint NOT NULL,
    type tinyint DEFAULT '4',
    title varchar(100) NOT NULL,
    content varchar(1000) DEFAULT NULL,
    biz_id bigint DEFAULT NULL,
    is_read tinyint DEFAULT '0',
    create_time timestamp DEFAULT NULL,
    PRIMARY KEY (id)
);
create index idx_message_receiver on message (receiver_id,receiver_type,is_read);

drop table if exists note;
create table note (
    id bigint NOT NULL auto_increment,
    user_id bigint NOT NULL,
    merchant_id bigint DEFAULT NULL,
    title varchar(100) DEFAULT NULL,
    content varchar(4000) DEFAULT NULL,
    images varchar(2000) DEFAULT NULL,
    cover_image varchar(255) DEFAULT NULL,
    view_count int DEFAULT '0',
    like_count int DEFAULT '0',
    status tinyint DEFAULT '0',
    create_time timestamp DEFAULT NULL,
    update_time timestamp DEFAULT NULL,
    deleted tinyint DEFAULT '0',
    PRIMARY KEY (id),
    KEY idx_note_user (user_id)
);
create index idx_note_merchant on note (merchant_id);

drop table if exists note_like;
create table note_like (
    id bigint NOT NULL auto_increment,
    user_id bigint NOT NULL,
    note_id bigint NOT NULL,
    create_time timestamp DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_note_like (user_id,note_id)
);
create index idx_note_like_note on note_like (note_id);

drop table if exists recommend_feedback;
create table recommend_feedback (
    id bigint NOT NULL auto_increment,
    user_id bigint NOT NULL,
    merchant_id bigint NOT NULL,
    feedback_type tinyint NOT NULL,
    source varchar(30) DEFAULT NULL,
    create_time timestamp DEFAULT NULL,
    PRIMARY KEY (id)
);
create unique index uk_feedback_user_merchant on recommend_feedback (user_id,merchant_id);

drop table if exists recommend_log;
create table recommend_log (
    id bigint NOT NULL auto_increment,
    user_id bigint DEFAULT NULL,
    merchant_id bigint NOT NULL,
    algorithm varchar(30) DEFAULT NULL,
    score decimal(6,2) DEFAULT NULL,
    reason varchar(255) DEFAULT NULL,
    request_id varchar(64) DEFAULT NULL,
    create_time timestamp DEFAULT NULL,
    PRIMARY KEY (id)
);
create index idx_log_user on recommend_log (user_id);

drop table if exists review;
create table review (
    id bigint NOT NULL auto_increment,
    user_id bigint NOT NULL,
    merchant_id bigint NOT NULL,
    dish_id bigint DEFAULT NULL,
    score int DEFAULT '5',
    content varchar(1000) DEFAULT NULL,
    images varchar(2000) DEFAULT NULL,
    status tinyint DEFAULT '0',
    like_count int DEFAULT '0',
    create_time timestamp DEFAULT NULL,
    update_time timestamp DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_review_user (user_id)
);
create index idx_review_merchant on review (merchant_id);

drop table if exists review_like;
create table review_like (
    id bigint NOT NULL auto_increment,
    user_id bigint NOT NULL,
    review_id bigint NOT NULL,
    create_time timestamp DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_like (user_id,review_id)
);
create index idx_review_like_review on review_like (review_id);

drop table if exists sys_admin;
create table sys_admin (
    id bigint NOT NULL auto_increment,
    username varchar(50) NOT NULL,
    password varchar(100) NOT NULL,
    nickname varchar(50) DEFAULT NULL,
    status tinyint DEFAULT '1',
    create_time timestamp DEFAULT NULL,
    update_time timestamp DEFAULT NULL,
    PRIMARY KEY (id)
);
create unique index uk_sys_admin_username on sys_admin (username);

drop table if exists sys_user;
create table sys_user (
    id bigint NOT NULL auto_increment,
    phone varchar(20) NOT NULL,
    password varchar(100) NOT NULL,
    nickname varchar(50) DEFAULT NULL,
    avatar varchar(255) DEFAULT NULL,
    contact_phone varchar(20) DEFAULT NULL,
    taste_cuisine varchar(100) DEFAULT NULL,
    taste_flavor varchar(20) DEFAULT NULL,
    taboo varchar(200) DEFAULT NULL,
    status tinyint DEFAULT '1',
    create_time timestamp DEFAULT NULL,
    update_time timestamp DEFAULT NULL,
    deleted tinyint DEFAULT '0',
    PRIMARY KEY (id)
);
create unique index uk_sys_user_phone on sys_user (phone);

drop table if exists user_coupon;
create table user_coupon (
    id bigint NOT NULL auto_increment,
    user_id bigint NOT NULL,
    coupon_id bigint NOT NULL,
    merchant_id bigint NOT NULL,
    status tinyint DEFAULT '0',
    redeem_code varchar(32) DEFAULT NULL,
    receive_time timestamp DEFAULT NULL,
    use_time timestamp DEFAULT NULL,
    expire_time timestamp DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_coupon (user_id,coupon_id),
    UNIQUE KEY uk_user_coupon_code (redeem_code)
);
create index idx_user_coupon_user on user_coupon (user_id,status);

drop table if exists wish_list;
create table wish_list (
    id bigint NOT NULL auto_increment,
    user_id bigint NOT NULL,
    merchant_id bigint NOT NULL,
    remark varchar(255) DEFAULT NULL,
    plan_date date DEFAULT NULL,
    create_time timestamp DEFAULT NULL,
    update_time timestamp DEFAULT NULL,
    PRIMARY KEY (id)
);
create unique index uk_wish_user_merchant on wish_list (user_id,merchant_id);
