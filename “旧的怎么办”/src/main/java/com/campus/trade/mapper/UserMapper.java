package com.campus.trade.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
    @Select("""
            SELECT id, openid, nickname, avatar_url AS avatarUrl, status
            FROM user_account
            WHERE openid = #{openid}
            LIMIT 1
            """)
    Map<String, Object> findByOpenid(@Param("openid") String openid);

    @Select("""
            SELECT id, openid, nickname, avatar_url AS avatarUrl, status
            FROM user_account
            WHERE id = #{userId}
            LIMIT 1
            """)
    Map<String, Object> findById(@Param("userId") Long userId);

    @Select("""
            SELECT
              u.id,
              COALESCE(u.openid, CONCAT('web_', u.id)) AS openid,
              u.nickname,
              u.avatar_url AS avatarUrl,
              u.status,
              ua.student_no AS studentNo,
              ua.salt,
              ua.password_md5 AS passwordHash,
              CASE WHEN u.openid LIKE 'wx_admin%' OR u.openid = 'web_admin' THEN TRUE ELSE FALSE END AS isAdmin
            FROM user_account u
            LEFT JOIN user_auth ua ON ua.user_id = u.id
            WHERE LOWER(u.nickname) = LOWER(#{nickname})
            LIMIT 1
            """)
    Map<String, Object> findPasswordLoginUserByNickname(@Param("nickname") String nickname);

    @Select("""
            SELECT
              u.id,
              COALESCE(u.openid, CONCAT('web_', u.id)) AS openid,
              u.nickname,
              u.avatar_url AS avatarUrl,
              u.status,
              ua.student_no AS studentNo,
              ua.salt,
              ua.password_md5 AS passwordHash,
              CASE WHEN u.openid LIKE 'wx_admin%' OR u.openid = 'web_admin' THEN TRUE ELSE FALSE END AS isAdmin
            FROM user_account u
            INNER JOIN user_auth ua ON ua.user_id = u.id
            WHERE ua.student_no = #{studentNo}
            LIMIT 1
            """)
    Map<String, Object> findPasswordLoginUserByStudentNo(@Param("studentNo") String studentNo);

    @Insert("""
            INSERT INTO user_account(openid, nickname, status, created_at, updated_at)
            VALUES(#{openid}, #{nickname}, 'ACTIVE', NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(Map<String, Object> user);

    @Insert("""
            INSERT INTO user_auth(user_id, salt, password_md5, created_at, updated_at)
            VALUES(#{userId}, #{salt}, #{passwordHash}, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
              salt = VALUES(salt),
              password_md5 = VALUES(password_md5),
              updated_at = NOW()
            """)
    int upsertUserPassword(
            @Param("userId") Long userId,
            @Param("salt") String salt,
            @Param("passwordHash") String passwordHash
    );

    @Insert("""
            INSERT INTO user_auth(user_id, student_no, campus, realname_status, verified_at, created_at, updated_at)
            VALUES(#{userId}, #{studentNo}, #{campus}, 'VERIFIED', NOW(), NOW(), NOW())
            ON DUPLICATE KEY UPDATE
              student_no = VALUES(student_no),
              campus = VALUES(campus),
              realname_status = 'VERIFIED',
              verified_at = NOW(),
              updated_at = NOW()
            """)
    int upsertUserAuth(@Param("userId") Long userId, @Param("studentNo") String studentNo, @Param("campus") String campus);

    @Select("""
            SELECT CASE WHEN realname_status = 'VERIFIED' THEN TRUE ELSE FALSE END
            FROM user_auth
            WHERE user_id = #{userId}
            LIMIT 1
            """)
    Boolean isCampusVerified(@Param("userId") Long userId);

    @Select("""
            SELECT
              u.id AS userId,
              u.nickname,
              u.avatar_url AS avatarUrl,
              u.status,
              ua.student_no AS studentNo,
              ua.campus,
              ua.verified_at AS verifiedAt,
              COALESCE(ua.realname_status, 'UNVERIFIED') AS verifyStatus,
              CASE WHEN ua.realname_status = 'VERIFIED' THEN TRUE ELSE FALSE END AS campusVerified,
              CASE WHEN u.openid LIKE 'wx_admin%' OR u.openid = 'web_admin' THEN TRUE ELSE FALSE END AS isAdmin,
              COALESCE(ROUND((SELECT AVG(r.score) FROM review r WHERE r.to_user = u.id), 1), 5.0) AS creditScore,
              (SELECT COUNT(*) FROM favorite f WHERE f.user_id = u.id) AS favoriteCount,
              (SELECT COUNT(*) FROM item i WHERE i.seller_id = u.id AND i.status <> 'DELETED') AS publishedItemCount
            FROM user_account u
            LEFT JOIN user_auth ua ON ua.user_id = u.id
            WHERE u.id = #{userId}
            LIMIT 1
            """)
    Map<String, Object> getProfile(@Param("userId") Long userId);

    @Select("""
            SELECT
              u.id AS userId,
              u.nickname,
              u.status,
              ua.student_no AS studentNo,
              ua.campus,
              CASE WHEN u.openid LIKE 'wx_admin%' OR u.openid = 'web_admin' THEN TRUE ELSE FALSE END AS isAdmin,
              CASE WHEN ua.realname_status = 'VERIFIED' THEN TRUE ELSE FALSE END AS campusVerified
            FROM user_account u
            LEFT JOIN user_auth ua ON ua.user_id = u.id
            ORDER BY u.id DESC
            """)
    List<Map<String, Object>> findAdminUsers();

    @Select("""
            SELECT CASE WHEN openid LIKE 'wx_admin%' OR openid = 'web_admin' THEN TRUE ELSE FALSE END
            FROM user_account
            WHERE id = #{userId}
            LIMIT 1
            """)
    Boolean isAdminUser(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM user_account
            WHERE LOWER(nickname) = LOWER(#{nickname})
            """)
    int countByNickname(@Param("nickname") String nickname);

    @Select("""
            SELECT COUNT(*)
            FROM user_account
            WHERE LOWER(nickname) = LOWER(#{nickname})
              AND id <> #{userId}
            """)
    int countByNicknameExcludeUser(@Param("nickname") String nickname, @Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM user_auth
            WHERE student_no = #{studentNo}
              AND user_id <> #{userId}
            """)
    int countByStudentNoExcludeUser(@Param("studentNo") String studentNo, @Param("userId") Long userId);

    @Select("""
            SELECT salt, password_md5 AS passwordHash
            FROM user_auth
            WHERE user_id = #{userId}
            LIMIT 1
            """)
    Map<String, Object> getPasswordAuth(@Param("userId") Long userId);

    @Update("""
            UPDATE user_account
            SET status = #{status}, updated_at = NOW()
            WHERE id = #{userId}
            """)
    int updateUserStatus(@Param("userId") Long userId, @Param("status") String status);

    @Update("""
            UPDATE user_account
            SET nickname = #{nickname}, updated_at = NOW()
            WHERE id = #{userId}
            """)
    int updateNickname(@Param("userId") Long userId, @Param("nickname") String nickname);

    @Update("""
            UPDATE user_account
            SET avatar_url = #{avatarUrl}, updated_at = NOW()
            WHERE id = #{userId}
            """)
    int updateAvatarUrl(@Param("userId") Long userId, @Param("avatarUrl") String avatarUrl);

    @Update("""
            UPDATE user_account
            SET status = 'DELETED',
                nickname = #{nickname},
                avatar_url = NULL,
                updated_at = NOW()
            WHERE id = #{userId}
            """)
    int markUserDeleted(@Param("userId") Long userId, @Param("nickname") String nickname);

    @Select("SELECT COUNT(*) FROM user_account")
    int countUsers();
}
