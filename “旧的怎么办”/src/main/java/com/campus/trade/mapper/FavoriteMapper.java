package com.campus.trade.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FavoriteMapper {
    @Select("""
            SELECT
              f.id,
              f.item_id AS itemId,
              i.title,
              i.price,
              c.name AS category,
              img.image_url AS imageUrl,
              f.created_at AS createdAt
            FROM favorite f
            LEFT JOIN item i ON i.id = f.item_id
            LEFT JOIN category c ON c.id = i.category_id
            LEFT JOIN item_image img ON img.id = (
              SELECT ii.id
              FROM item_image ii
              WHERE ii.item_id = i.id
              ORDER BY ii.sort_no ASC, ii.id ASC
              LIMIT 1
            )
            WHERE f.user_id = #{userId}
              AND i.status = 'ON_SALE'
            ORDER BY f.created_at DESC
            """)
    List<Map<String, Object>> findFavorites(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO favorite(user_id, item_id, created_at)
            VALUES(#{userId}, #{itemId}, NOW())
            """)
    int insertFavorite(@Param("userId") Long userId, @Param("itemId") Long itemId);

    @Delete("""
            DELETE FROM favorite
            WHERE user_id = #{userId} AND item_id = #{itemId}
            """)
    int deleteFavorite(@Param("userId") Long userId, @Param("itemId") Long itemId);
}
