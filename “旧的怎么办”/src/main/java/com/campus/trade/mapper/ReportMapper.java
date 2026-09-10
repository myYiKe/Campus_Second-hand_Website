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
public interface ReportMapper {
    @Select("""
            SELECT
              r.id,
              r.target_type AS targetType,
              r.target_id AS targetId,
              r.reason,
              r.status,
              r.handle_result AS handleResult,
              r.created_at AS createdAt,
              reporter.nickname AS reporterName,
              i.title AS itemTitle,
              i.description AS itemDescription,
              i.price AS itemPrice,
              i.stock AS itemStock,
              i.status AS itemStatus,
              c.name AS itemCategory,
              img.image_url AS itemImageUrl,
              ua.campus AS itemCampus,
              i.created_at AS itemPublishTime,
              i.seller_id AS sellerId,
              seller.nickname AS sellerName
            FROM report_record r
            LEFT JOIN user_account reporter ON reporter.id = r.reporter_id
            LEFT JOIN item i ON i.id = r.target_id AND r.target_type = 'ITEM'
            LEFT JOIN category c ON c.id = i.category_id
            LEFT JOIN user_auth ua ON ua.user_id = i.seller_id
            LEFT JOIN user_account seller ON seller.id = i.seller_id
            LEFT JOIN (
              SELECT item_id, MIN(image_url) AS image_url
              FROM item_image
              GROUP BY item_id
            ) img ON img.item_id = i.id
            ORDER BY CASE WHEN r.status = 'PENDING' THEN 0 ELSE 1 END, r.created_at DESC
            """)
    List<Map<String, Object>> findReports();

    @Insert("""
            INSERT INTO report_record(reporter_id, target_type, target_id, reason, status, created_at)
            VALUES(#{reporterId}, #{targetType}, #{targetId}, #{reason}, 'PENDING', NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReport(Map<String, Object> report);

    @Update("""
            UPDATE report_record
            SET status = #{status},
                handle_result = #{handleResult}
            WHERE target_type = #{targetType}
              AND target_id = #{targetId}
              AND status = 'PENDING'
            """)
    int updateReportsByTarget(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("status") String status,
            @Param("handleResult") String handleResult
    );

    @Select("SELECT COUNT(*) FROM report_record WHERE status = 'PENDING'")
    int countReports();
}
