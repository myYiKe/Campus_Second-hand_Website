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
public interface ItemMapper {
    @Select("""
            <script>
            SELECT
              i.id,
              i.seller_id AS sellerId,
              i.title,
              i.price,
              i.stock,
              i.description,
              c.name AS category,
              i.status,
              u.nickname AS sellerName,
              CASE
                WHEN ua.campus = '望江校区' THEN '望江校区'
                WHEN ua.campus = '江安校区' THEN '江安校区'
                ELSE ua.campus
              END AS campus,
              i.created_at AS publishTime,
              (SELECT COUNT(*) FROM favorite f WHERE f.item_id = i.id) AS favoriteCount,
              img.imageUrl AS imageUrl,
              img.imageUrls AS imageUrls
            FROM item i
            LEFT JOIN category c ON c.id = i.category_id
            LEFT JOIN user_account u ON u.id = i.seller_id
            LEFT JOIN user_auth ua ON ua.user_id = i.seller_id
            LEFT JOIN (
              SELECT
                item_id,
                MIN(image_url) AS imageUrl,
                GROUP_CONCAT(image_url ORDER BY sort_no SEPARATOR '||') AS imageUrls
              FROM item_image
              GROUP BY item_id
            ) img ON img.item_id = i.id
            WHERE i.status = 'ON_SALE'
            <if test="keyword != null and keyword != ''">
              AND (
                i.title LIKE CONCAT('%', #{keyword}, '%')
                OR REPLACE(i.title, ' ', '') LIKE CONCAT('%', REPLACE(#{keyword}, ' ', ''), '%')
                OR i.description LIKE CONCAT('%', #{keyword}, '%')
                OR c.name LIKE CONCAT('%', #{keyword}, '%')
                OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            <if test="category != null and category != ''">
              AND c.name = #{category}
            </if>
            <if test="campus != null and campus != ''">
              AND CASE
                WHEN ua.campus = '望江校区' THEN '望江校区'
                WHEN ua.campus = '江安校区' THEN '江安校区'
                ELSE ua.campus
              END = #{campus}
            </if>
            ORDER BY i.created_at DESC
            </script>
            """)
    List<Map<String, Object>> findItems(@Param("keyword") String keyword, @Param("category") String category, @Param("campus") String campus);

    @Select("""
            SELECT id, seller_id AS sellerId, price, title, status, stock
            FROM item
            WHERE id = #{itemId}
            LIMIT 1
            """)
    Map<String, Object> findItemBase(@Param("itemId") Long itemId);

    @Select("""
            SELECT
              i.id,
              i.seller_id AS sellerId,
              i.title,
              i.price,
              i.stock,
              i.description,
              c.name AS category,
              i.status,
              u.nickname AS sellerName,
              CASE
                WHEN ua.campus = '望江校区' THEN '望江校区'
                WHEN ua.campus = '江安校区' THEN '江安校区'
                ELSE ua.campus
              END AS campus,
              i.created_at AS publishTime,
              (SELECT COUNT(*) FROM favorite f WHERE f.item_id = i.id) AS favoriteCount,
              img.imageUrl AS imageUrl,
              img.imageUrls AS imageUrls
            FROM item i
            LEFT JOIN category c ON c.id = i.category_id
            LEFT JOIN user_account u ON u.id = i.seller_id
            LEFT JOIN user_auth ua ON ua.user_id = i.seller_id
            LEFT JOIN (
              SELECT
                item_id,
                MIN(image_url) AS imageUrl,
                GROUP_CONCAT(image_url ORDER BY sort_no SEPARATOR '||') AS imageUrls
              FROM item_image
              GROUP BY item_id
            ) img ON img.item_id = i.id
            WHERE i.id = #{itemId}
            LIMIT 1
            """)
    Map<String, Object> findItemDetail(@Param("itemId") Long itemId);

    @Select("""
            SELECT
              id,
              title,
              price,
              stock,
              status,
              description,
              (
                SELECT image_url
                FROM item_image
                WHERE item_id = item.id
                ORDER BY sort_no ASC, id ASC
                LIMIT 1
              ) AS imageUrl,
              updated_at AS updatedAt
            FROM item
            WHERE seller_id = #{sellerId}
              AND status <> 'DELETED'
            ORDER BY updated_at DESC
            """)
    List<Map<String, Object>> findMyItems(@Param("sellerId") Long sellerId);

    @Select("""
            SELECT id
            FROM category
            WHERE name = #{name}
            LIMIT 1
            """)
    Long findCategoryIdByName(@Param("name") String name);

    @Insert("""
            INSERT INTO category(name, parent_id, sort_no)
            VALUES(#{name}, 0, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCategory(Map<String, Object> category);

    @Insert("""
            INSERT INTO item(seller_id, category_id, title, description, price, stock, status, audit_status, version, created_at, updated_at)
            VALUES(#{sellerId}, #{categoryId}, #{title}, #{description}, #{price}, #{stock}, 'ON_SALE', 'APPROVED', 0, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertItem(Map<String, Object> item);

    @Insert("""
            INSERT INTO item_image(item_id, image_url, sort_no)
            VALUES(#{itemId}, #{imageUrl}, #{sortNo})
            """)
    int insertItemImage(@Param("itemId") Long itemId, @Param("imageUrl") String imageUrl, @Param("sortNo") int sortNo);

    @Select("""
            SELECT id, stock, status
            FROM item
            WHERE id = #{itemId} AND seller_id = #{sellerId}
            LIMIT 1
            """)
    Map<String, Object> findMyItemBase(@Param("itemId") Long itemId, @Param("sellerId") Long sellerId);

    @Update("""
            UPDATE item
            SET status = #{status}, updated_at = NOW()
            WHERE id = #{itemId} AND seller_id = #{sellerId}
            """)
    int updateMyItemStatus(@Param("itemId") Long itemId, @Param("sellerId") Long sellerId, @Param("status") String status);

    @Update("""
            UPDATE item
            SET stock = stock - 1,
                status = CASE WHEN stock <= 1 THEN 'SOLD' ELSE status END,
                updated_at = NOW()
            WHERE id = #{itemId} AND status = 'ON_SALE' AND stock > 0
            """)
    int decreaseStockForOrder(@Param("itemId") Long itemId);

    @Update("""
            UPDATE item
            SET stock = stock + 1,
                status = CASE WHEN status = 'SOLD' THEN 'ON_SALE' ELSE status END,
                updated_at = NOW()
            WHERE id = #{itemId}
              AND status <> 'DELETED'
            """)
    int restoreStockForOrder(@Param("itemId") Long itemId);

    @Update("""
            UPDATE item
            SET audit_status = #{result},
                audit_reason = #{reason},
                status = CASE WHEN #{result} = 'APPROVED' THEN 'ON_SALE' ELSE 'REJECTED' END,
                updated_at = NOW()
            WHERE id = #{itemId}
            """)
    int updateAudit(@Param("itemId") Long itemId, @Param("result") String result, @Param("reason") String reason);

    @Update("""
            UPDATE item
            SET status = 'PENDING_REVIEW',
                audit_status = 'REPORTED',
                audit_reason = #{reason},
                updated_at = NOW()
            WHERE id = #{itemId}
              AND status = 'ON_SALE'
            """)
    int hideItemForReport(@Param("itemId") Long itemId, @Param("reason") String reason);

    @Update("""
            UPDATE item
            SET status = 'DELETED', updated_at = NOW()
            WHERE id = #{itemId} AND seller_id = #{sellerId}
            """)
    int deleteMyItem(@Param("itemId") Long itemId, @Param("sellerId") Long sellerId);

    @Update("""
            UPDATE item
            SET status = 'DELETED',
                audit_status = 'REJECTED',
                audit_reason = '账号已被管理员删除',
                updated_at = NOW()
            WHERE seller_id = #{sellerId}
              AND status <> 'DELETED'
            """)
    int deleteItemsBySeller(@Param("sellerId") Long sellerId);

    @Select("SELECT COUNT(*) FROM item")
    int countItems();

    @Select("SELECT COUNT(*) FROM item WHERE status = 'PENDING_REVIEW'")
    int countPendingAudit();
}
