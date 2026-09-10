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
public interface MessageMapper {
    @Select("""
            SELECT id
            FROM conversation
            WHERE (user_a = #{userA} AND user_b = #{userB})
               OR (user_a = #{userB} AND user_b = #{userA})
            ORDER BY id ASC
            LIMIT 1
            """)
    Long findConversationId(@Param("userA") Long userA, @Param("userB") Long userB);

    @Insert("""
            INSERT INTO conversation(user_a, user_b, last_message_at, created_at)
            VALUES(#{userA}, #{userB}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertConversation(Map<String, Object> conversation);

    @Update("""
            UPDATE conversation
            SET last_message_at = NOW()
            WHERE id = #{conversationId}
            """)
    int touchConversation(@Param("conversationId") Long conversationId);

    @Select("""
            <script>
            SELECT
              m.id,
              m.order_id AS orderId,
              m.item_id AS itemId,
              m.from_user AS fromUserId,
              m.to_user AS toUserId,
              CASE
                WHEN m.content LIKE 'REVIEW::%' THEN 'REVIEW'
                WHEN m.content LIKE 'DISPUTE::%' THEN 'DISPUTE'
                ELSE 'TEXT'
              END AS messageType,
              CASE
                WHEN m.content LIKE 'REVIEW::%' THEN SUBSTRING(m.content, 9)
                WHEN m.content LIKE 'DISPUTE::%' THEN SUBSTRING(m.content, 10)
                ELSE m.content
              END AS content,
              m.created_at AS createdAt,
              fu.nickname AS fromName,
              tu.nickname AS toName,
              COALESCE(i.title, oi.title) AS itemTitle
            FROM message m
            LEFT JOIN user_account fu ON fu.id = m.from_user
            LEFT JOIN user_account tu ON tu.id = m.to_user
            LEFT JOIN item i ON i.id = m.item_id
            LEFT JOIN trade_order o ON o.id = m.order_id
            LEFT JOIN item oi ON oi.id = o.item_id
            LEFT JOIN user_hidden_thread ht
              ON ht.user_id = #{userId}
             AND ht.order_id = IFNULL(m.order_id, 0)
             AND ht.item_id = IFNULL(m.item_id, 0)
            WHERE (m.from_user = #{userId} OR m.to_user = #{userId})
              AND (ht.hidden_before IS NULL OR m.created_at > ht.hidden_before)
            <if test="orderId != null">
              AND m.order_id = #{orderId}
            </if>
            <if test="itemId != null">
              AND m.item_id = #{itemId}
            </if>
            ORDER BY m.created_at DESC
            </script>
            """)
    List<Map<String, Object>> findVisibleMessages(
            @Param("userId") Long userId,
            @Param("orderId") Long orderId,
            @Param("itemId") Long itemId
    );

    @Insert("""
            INSERT INTO message(conversation_id, from_user, to_user, order_id, item_id, message_type, content, read_status, created_at)
            VALUES(#{conversationId}, #{fromUser}, #{toUser}, #{orderId}, #{itemId}, #{messageType}, #{content}, 0, NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMessage(Map<String, Object> message);

    @Insert("""
            INSERT INTO user_hidden_thread(user_id, order_id, item_id, hidden_before, updated_at)
            VALUES(#{userId}, #{orderId}, #{itemId}, NOW(), NOW())
            ON DUPLICATE KEY UPDATE hidden_before = NOW(), updated_at = NOW()
            """)
    int upsertHiddenThread(@Param("userId") Long userId, @Param("orderId") Long orderId, @Param("itemId") Long itemId);
}
