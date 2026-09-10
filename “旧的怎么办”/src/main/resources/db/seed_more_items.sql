INSERT INTO item (seller_id, category_id, title, description, price, status, audit_status, audit_reason, version, created_at, updated_at)
SELECT 11, 1, 'MacBook Air M2 13 8+256', '2023 年购入，电池健康良好，原装充电器齐全，适合日常学习和轻办公。', 5688.00, 'ON_SALE', 'APPROVED', NULL, 0, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'MacBook Air M2 13 8+256');

INSERT INTO item (seller_id, category_id, title, description, price, status, audit_status, audit_reason, version, created_at, updated_at)
SELECT 10, 1, 'Logitech MX Master 3S Mouse', '罗技无线鼠标，蓝牙和接收器齐全，按键顺滑，成色干净。', 369.00, 'ON_SALE', 'APPROVED', NULL, 0, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'Logitech MX Master 3S Mouse');

INSERT INTO item (seller_id, category_id, title, description, price, status, audit_status, audit_reason, version, created_at, updated_at)
SELECT 11, 2, 'CET-6 Study Bundle 2024', '四六级资料整套转让，含真题、作文模板和高频词汇笔记。', 39.00, 'ON_SALE', 'APPROVED', NULL, 0, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'CET-6 Study Bundle 2024');

INSERT INTO item (seller_id, category_id, title, description, price, status, audit_status, audit_reason, version, created_at, updated_at)
SELECT 10, 1, 'Xiaomi Monitor Light Bar', '小米屏幕挂灯，亮度可调，适合宿舍书桌和晚间自习。', 79.00, 'ON_SALE', 'APPROVED', NULL, 0, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'Xiaomi Monitor Light Bar');

INSERT INTO item (seller_id, category_id, title, description, price, status, audit_status, audit_reason, version, created_at, updated_at)
SELECT 11, 3, 'IKEA Adjustable Desk 120cm', '桌面平整稳固，适合宿舍学习和摆放电脑，使用痕迹轻。', 228.00, 'ON_SALE', 'APPROVED', NULL, 0, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'IKEA Adjustable Desk 120cm');

INSERT INTO item (seller_id, category_id, title, description, price, status, audit_status, audit_reason, version, created_at, updated_at)
SELECT 10, 1, 'Apple Watch S8 45mm', '附两条表带，电池状态正常，屏幕和边框成色良好。', 1288.00, 'ON_SALE', 'APPROVED', NULL, 0, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'Apple Watch S8 45mm');

INSERT INTO item (seller_id, category_id, title, description, price, status, audit_status, audit_reason, version, created_at, updated_at)
SELECT 11, 1, 'Sony WH-1000XM4 Headphones', '降噪功能正常，附原装收纳盒，适合自习和通勤使用。', 999.00, 'ON_SALE', 'APPROVED', NULL, 0, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'Sony WH-1000XM4 Headphones');

INSERT INTO item_image (item_id, image_url, sort_no)
SELECT i.id, 'https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt=realistic%20silver%20macbook%20air%20m2%20laptop%20on%20a%20clean%20wooden%20desk%20in%20a%20college%20dorm%2C%20product%20photography&image_size=landscape_4_3', 0
FROM item i
WHERE i.title = 'MacBook Air M2 13 8+256'
  AND NOT EXISTS (SELECT 1 FROM item_image ii WHERE ii.item_id = i.id);

INSERT INTO item_image (item_id, image_url, sort_no)
SELECT i.id, 'https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt=realistic%20logitech%20mx%20master%203s%20mouse%20on%20a%20matte%20desk%20surface%2C%20close%20up%20product%20photo%2C%20soft%20lighting&image_size=landscape_4_3', 0
FROM item i
WHERE i.title = 'Logitech MX Master 3S Mouse'
  AND NOT EXISTS (SELECT 1 FROM item_image ii WHERE ii.item_id = i.id);

INSERT INTO item_image (item_id, image_url, sort_no)
SELECT i.id, 'https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt=realistic%20english%20exam%20study%20books%2C%20vocabulary%20cards%2C%20and%20papers%20arranged%20neatly%20on%20a%20desk%2C%20product%20photo&image_size=landscape_4_3', 0
FROM item i
WHERE i.title = 'CET-6 Study Bundle 2024'
  AND NOT EXISTS (SELECT 1 FROM item_image ii WHERE ii.item_id = i.id);

INSERT INTO item_image (item_id, image_url, sort_no)
SELECT i.id, 'https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt=realistic%20monitor%20light%20bar%20mounted%20above%20a%20computer%20screen%20on%20a%20clean%20study%20desk%2C%20product%20photo&image_size=landscape_4_3', 0
FROM item i
WHERE i.title = 'Xiaomi Monitor Light Bar'
  AND NOT EXISTS (SELECT 1 FROM item_image ii WHERE ii.item_id = i.id);

INSERT INTO item_image (item_id, image_url, sort_no)
SELECT i.id, 'https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt=realistic%20height%20adjustable%20wooden%20computer%20desk%20in%20a%20simple%20student%20room%2C%20product%20photo&image_size=landscape_4_3', 0
FROM item i
WHERE i.title = 'IKEA Adjustable Desk 120cm'
  AND NOT EXISTS (SELECT 1 FROM item_image ii WHERE ii.item_id = i.id);

INSERT INTO item_image (item_id, image_url, sort_no)
SELECT i.id, 'https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt=realistic%20apple%20watch%20series%208%20with%20black%20sport%20band%20on%20a%20clean%20table%2C%20close%20up%20product%20photography&image_size=square_hd', 0
FROM item i
WHERE i.title = 'Apple Watch S8 45mm'
  AND NOT EXISTS (SELECT 1 FROM item_image ii WHERE ii.item_id = i.id);

INSERT INTO item_image (item_id, image_url, sort_no)
SELECT i.id, 'https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt=realistic%20sony%20wh-1000xm4%20headphones%20with%20case%20on%20a%20wood%20desk%2C%20premium%20product%20photo%2C%20soft%20light&image_size=landscape_4_3', 0
FROM item i
WHERE i.title = 'Sony WH-1000XM4 Headphones'
  AND NOT EXISTS (SELECT 1 FROM item_image ii WHERE ii.item_id = i.id);
