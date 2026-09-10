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
public interface OrderMapper {
    @Select("""
            <script>
            SELECT
              o.id,
              o.item_id AS itemId,
              o.buyer_id AS buyerId,
              o.seller_id AS sellerId,
              i.title AS itemTitle,
              bu.nickname AS buyerName,
              su.nickname AS sellerName,
              o.amount,
              o.receiver_name AS receiverName,
              o.receiver_phone AS receiverPhone,
              o.receiver_campus AS receiverCampus,
              o.receiver_detail AS receiverDetail,
              o.payment_method AS paymentMethod,
              o.payment_no AS paymentNo,
              o.cancel_reason AS cancelReason,
              o.status,
              o.paid_at AS paidAt,
              o.shipped_at AS shippedAt,
              o.completed_at AS completedAt,
              o.created_at AS createdAt
            FROM trade_order o
            LEFT JOIN item i ON i.id = o.item_id
            LEFT JOIN user_account bu ON bu.id = o.buyer_id
            LEFT JOIN user_account su ON su.id = o.seller_id
            WHERE (o.buyer_id = #{userId} OR o.seller_id = #{userId})
              AND NOT EXISTS (
                SELECT 1
                FROM user_hidden_order uho
                WHERE uho.user_id = #{userId}
                  AND uho.order_id = o.id
              )
            <if test="status != null and status != ''">
              AND o.status = #{status}
            </if>
            ORDER BY o.created_at DESC
            </script>
            """)
    List<Map<String, Object>> findOrders(@Param("userId") Long userId, @Param("status") String status);

    @Insert("""
            INSERT INTO trade_order(
              item_id, buyer_id, seller_id, amount,
              receiver_name, receiver_phone, receiver_campus, receiver_detail,
              status, created_at, updated_at
            )
            VALUES(
              #{itemId}, #{buyerId}, #{sellerId}, #{amount},
              #{receiverName}, #{receiverPhone}, #{receiverCampus}, #{receiverDetail},
              'PENDING_PAYMENT', NOW(), NOW()
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrder(Map<String, Object> order);

    @Select("""
            SELECT
              o.id,
              o.item_id AS itemId,
              o.buyer_id AS buyerId,
              o.seller_id AS sellerId,
              i.title AS itemTitle,
              o.amount,
              o.receiver_name AS receiverName,
              o.receiver_phone AS receiverPhone,
              o.receiver_campus AS receiverCampus,
              o.receiver_detail AS receiverDetail,
              o.payment_method AS paymentMethod,
              o.payment_no AS paymentNo,
              o.cancel_reason AS cancelReason,
              o.status,
              o.paid_at AS paidAt,
              o.shipped_at AS shippedAt,
              o.completed_at AS completedAt,
              o.created_at AS createdAt
            FROM trade_order o
            LEFT JOIN item i ON i.id = o.item_id
            WHERE o.id = #{orderId}
            LIMIT 1
            """)
    Map<String, Object> findOrderBase(@Param("orderId") Long orderId);

    @Select("""
            SELECT COUNT(*)
            FROM review
            WHERE order_id = #{orderId} AND from_user = #{fromUser}
            """)
    int countReviewsByOrderAndUser(@Param("orderId") Long orderId, @Param("fromUser") Long fromUser);

    @Select("""
            SELECT COUNT(*)
            FROM trade_order
            WHERE item_id = #{itemId}
              AND buyer_id = #{buyerId}
              AND status IN ('PENDING_PAYMENT', 'PAID', 'SHIPPED', 'COMPLETED', 'DISPUTING')
            """)
    int countActiveOrdersByItemAndBuyer(@Param("itemId") Long itemId, @Param("buyerId") Long buyerId);

    @Update("""
            UPDATE trade_order
            SET status = 'PAID',
                payment_method = #{paymentMethod},
                payment_no = #{paymentNo},
                paid_at = NOW(),
                updated_at = NOW()
            WHERE id = #{orderId}
              AND status = 'PENDING_PAYMENT'
            """)
    int payOrder(@Param("orderId") Long orderId, @Param("paymentMethod") String paymentMethod, @Param("paymentNo") String paymentNo);

    @Update("""
            UPDATE trade_order
            SET status = 'SHIPPED',
                shipped_at = NOW(),
                updated_at = NOW()
            WHERE id = #{orderId}
              AND status = 'PAID'
            """)
    int shipOrder(@Param("orderId") Long orderId);

    @Update("""
            UPDATE trade_order
            SET status = 'COMPLETED',
                completed_at = NOW(),
                updated_at = NOW()
            WHERE id = #{orderId}
              AND status IN ('SHIPPED', 'DISPUTING')
            """)
    int completeOrder(@Param("orderId") Long orderId);

    @Update("""
            UPDATE trade_order
            SET status = 'CANCELLED',
                cancel_reason = #{cancelReason},
                updated_at = NOW()
            WHERE id = #{orderId}
              AND status = 'PENDING_PAYMENT'
            """)
    int cancelOrder(@Param("orderId") Long orderId, @Param("cancelReason") String cancelReason);

    @Select("""
            SELECT
              id,
              item_id AS itemId,
              buyer_id AS buyerId,
              seller_id AS sellerId
            FROM trade_order
            WHERE status = 'PENDING_PAYMENT'
              AND created_at <= DATE_SUB(NOW(), INTERVAL 30 MINUTE)
            ORDER BY created_at ASC
            """)
    List<Map<String, Object>> findExpiredPendingPaymentOrders();

    @Update("""
            UPDATE trade_order
            SET status = 'PAYMENT_FAILED',
                cancel_reason = #{cancelReason},
                updated_at = NOW()
            WHERE id = #{orderId}
              AND status = 'PENDING_PAYMENT'
              AND created_at <= DATE_SUB(NOW(), INTERVAL 30 MINUTE)
            """)
    int failExpiredOrder(@Param("orderId") Long orderId, @Param("cancelReason") String cancelReason);

    @Update("""
            UPDATE trade_order
            SET status = #{status},
                updated_at = NOW()
            WHERE id = #{orderId}
            """)
    int updateOrderStatus(@Param("orderId") Long orderId, @Param("status") String status);

    @Select("""
            SELECT
              r.id,
              r.order_id AS orderId,
              r.score,
              r.content,
              fu.nickname AS fromUser,
              tu.nickname AS toUser,
              r.created_at AS createdAt
            FROM review r
            LEFT JOIN user_account fu ON fu.id = r.from_user
            LEFT JOIN user_account tu ON tu.id = r.to_user
            ORDER BY r.created_at DESC
            """)
    List<Map<String, Object>> findReviews();

    @Select("""
            SELECT
              r.id,
              r.order_id AS orderId,
              o.item_id AS itemId,
              i.title AS itemTitle,
              o.buyer_id AS buyerId,
              o.seller_id AS sellerId,
              r.from_user AS fromUserId,
              r.to_user AS toUserId,
              r.score,
              r.content,
              fu.nickname AS fromUser,
              tu.nickname AS toUser,
              r.created_at AS createdAt
            FROM review r
            LEFT JOIN trade_order o ON o.id = r.order_id
            LEFT JOIN item i ON i.id = o.item_id
            LEFT JOIN user_account fu ON fu.id = r.from_user
            LEFT JOIN user_account tu ON tu.id = r.to_user
            WHERE o.buyer_id = #{userId} OR o.seller_id = #{userId}
            ORDER BY r.created_at DESC
            """)
    List<Map<String, Object>> findReviewsByUser(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO review(order_id, from_user, to_user, score, content, created_at)
            VALUES(#{orderId}, #{fromUser}, #{toUser}, #{score}, #{content}, NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReview(Map<String, Object> review);

    @Select("""
            SELECT
              d.id,
              d.order_id AS orderId,
              o.item_id AS itemId,
              i.title AS itemTitle,
              o.buyer_id AS buyerId,
              o.seller_id AS sellerId,
              bu.nickname AS buyerName,
              su.nickname AS sellerName,
              u.nickname AS complainant,
              d.complainant_id AS complainantId,
              d.reason,
              d.order_status_snapshot AS orderStatusSnapshot,
              d.status,
              d.handle_result AS handleResult,
              d.handler_id AS handlerId,
              hu.nickname AS handlerName,
              d.created_at AS createdAt,
              d.handled_at AS handledAt
            FROM dispute_record d
            LEFT JOIN trade_order o ON o.id = d.order_id
            LEFT JOIN item i ON i.id = o.item_id
            LEFT JOIN user_account bu ON bu.id = o.buyer_id
            LEFT JOIN user_account su ON su.id = o.seller_id
            LEFT JOIN user_account u ON u.id = d.complainant_id
            LEFT JOIN user_account hu ON hu.id = d.handler_id
            ORDER BY d.created_at DESC
            """)
    List<Map<String, Object>> findDisputes();

    @Select("""
            SELECT
              d.id,
              d.order_id AS orderId,
              o.item_id AS itemId,
              i.title AS itemTitle,
              o.buyer_id AS buyerId,
              o.seller_id AS sellerId,
              bu.nickname AS buyerName,
              su.nickname AS sellerName,
              d.complainant_id AS complainantId,
              u.nickname AS complainant,
              d.reason,
              d.order_status_snapshot AS orderStatusSnapshot,
              d.status,
              d.handle_result AS handleResult,
              d.handler_id AS handlerId,
              hu.nickname AS handlerName,
              d.created_at AS createdAt,
              d.handled_at AS handledAt
            FROM dispute_record d
            LEFT JOIN trade_order o ON o.id = d.order_id
            LEFT JOIN item i ON i.id = o.item_id
            LEFT JOIN user_account bu ON bu.id = o.buyer_id
            LEFT JOIN user_account su ON su.id = o.seller_id
            LEFT JOIN user_account u ON u.id = d.complainant_id
            LEFT JOIN user_account hu ON hu.id = d.handler_id
            WHERE o.buyer_id = #{userId} OR o.seller_id = #{userId}
            ORDER BY d.created_at DESC
            """)
    List<Map<String, Object>> findDisputesByUser(@Param("userId") Long userId);

    @Select("""
            SELECT
              d.id,
              d.order_id AS orderId,
              d.complainant_id AS complainantId,
              d.reason,
              d.order_status_snapshot AS orderStatusSnapshot,
              d.status,
              d.handle_result AS handleResult,
              d.handler_id AS handlerId,
              d.created_at AS createdAt,
              d.handled_at AS handledAt
            FROM dispute_record d
            WHERE d.id = #{disputeId}
            LIMIT 1
            """)
    Map<String, Object> findDisputeById(@Param("disputeId") Long disputeId);

    @Select("""
            SELECT COUNT(*)
            FROM dispute_record
            WHERE order_id = #{orderId}
              AND status IN ('PENDING', 'PROCESSING')
            """)
    int countOpenDisputesByOrder(@Param("orderId") Long orderId);

    @Insert("""
            INSERT INTO dispute_record(order_id, complainant_id, reason, order_status_snapshot, status, created_at)
            VALUES(#{orderId}, #{complainantId}, #{reason}, #{orderStatusSnapshot}, 'PENDING', NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDispute(Map<String, Object> dispute);

    @Update("""
            UPDATE dispute_record
            SET status = #{status},
                handle_result = #{handleResult},
                handler_id = #{handlerId},
                handled_at = CASE WHEN #{status} = 'PROCESSING' THEN NULL ELSE NOW() END
            WHERE id = #{disputeId}
            """)
    int handleDispute(
            @Param("disputeId") Long disputeId,
            @Param("status") String status,
            @Param("handleResult") String handleResult,
            @Param("handlerId") Long handlerId
    );

    @Select("SELECT COUNT(*) FROM trade_order")
    int countOrders();

    @Select("SELECT COUNT(*) FROM dispute_record WHERE status IN ('PENDING', 'PROCESSING')")
    int countPendingDisputes();

    @Insert("""
            INSERT INTO user_hidden_order(user_id, order_id, created_at)
            VALUES(#{userId}, #{orderId}, NOW())
            ON DUPLICATE KEY UPDATE created_at = NOW()
            """)
    int hideOrderForUser(@Param("userId") Long userId, @Param("orderId") Long orderId);

    @Insert("""
            INSERT INTO user_hidden_order(user_id, order_id, created_at)
            SELECT #{userId}, o.id, NOW()
            FROM trade_order o
            LEFT JOIN item i ON i.id = o.item_id
            WHERE (o.buyer_id = #{userId} OR o.seller_id = #{userId})
              AND (i.id IS NULL OR i.status IN ('OFF_SHELF', 'DELETED', 'REJECTED', 'PENDING_REVIEW'))
              AND NOT EXISTS (
                SELECT 1
                FROM user_hidden_order uho
                WHERE uho.user_id = #{userId}
                  AND uho.order_id = o.id
              )
            """)
    int hideInvalidOrdersForUser(@Param("userId") Long userId);
}
