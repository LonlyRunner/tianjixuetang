-- Fix historical image URLs that depend on the /img-tx Nginx proxy.
-- COS source has been verified to be publicly reachable.
SET @img_domain := 'https://wisehub-1312394356.cos.ap-shanghai.myqcloud.com/';

USE tj_course;
UPDATE course
SET cover_url = REPLACE(cover_url, '/img-tx/', @img_domain)
WHERE cover_url LIKE '/img-tx/%';

UPDATE course_draft
SET cover_url = REPLACE(cover_url, '/img-tx/', @img_domain)
WHERE cover_url LIKE '/img-tx/%';

USE tj_trade;
UPDATE cart
SET cover_url = REPLACE(cover_url, '/img-tx/', @img_domain)
WHERE cover_url LIKE '/img-tx/%';

UPDATE order_detail
SET cover_url = REPLACE(cover_url, '/img-tx/', @img_domain)
WHERE cover_url LIKE '/img-tx/%';

USE tj_user;
UPDATE user_detail
SET icon = REPLACE(icon, '/img-tx/', @img_domain)
WHERE icon LIKE '/img-tx/%';

UPDATE user_detail
SET photo = REPLACE(photo, '/img-tx/', @img_domain)
WHERE photo LIKE '/img-tx/%';

USE tj_pay;
UPDATE pay_channel
SET channel_icon = REPLACE(channel_icon, '/img-tx/', @img_domain)
WHERE channel_icon LIKE '/img-tx/%';
