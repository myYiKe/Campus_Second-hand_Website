package com.campus.trade.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HistoryMapper {
    @Select("""
            SELECT
              h.id,
              h.item_id AS itemId,
              i.title,
              c.name AS category,
              img.image_url AS imageUrl,
              h.browsed_at AS browsedAt
            FROM browse_history h
            LEFT JOIN item i ON i.id = h.item_id
            LEFT JOIN category c ON c.id = i.category_id
            LEFT JOIN item_image img ON img.id = (
              SELECT ii.id
              FROM item_image ii
              WHERE ii.item_id = i.id
              ORDER BY ii.sort_no ASC, ii.id ASC
              LIMIT 1
            )
            WHERE h.user_id = #{userId}
              AND i.status = 'ON_SALE'
            ORDER BY h.browsed_at DESC
            """)
    List<Map<String, Object>> findBrowseHistory(@Param("userId") Long userId);
}
