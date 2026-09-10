package com.campus.trade.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WantedMapper {
    @Select("""
            SELECT
              w.id,
              w.title,
              w.description,
              w.budget,
              u.nickname AS publisherName,
              w.created_at AS createdAt
            FROM wanted_post w
            LEFT JOIN user_account u ON u.id = w.user_id
            ORDER BY w.created_at DESC
            """)
    List<Map<String, Object>> findWantedPosts();

    @Insert("""
            INSERT INTO wanted_post(user_id, title, description, budget, status, created_at)
            VALUES(#{userId}, #{title}, #{description}, #{budget}, 'OPEN', NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertWantedPost(Map<String, Object> wantedPost);
}
