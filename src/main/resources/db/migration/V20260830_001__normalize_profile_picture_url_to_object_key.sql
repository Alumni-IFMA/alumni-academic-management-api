UPDATE users
SET profile_picture_url = substring(profile_picture_url from position('profile-pictures/' in profile_picture_url))
WHERE profile_picture_url LIKE '%profile-pictures/%';
