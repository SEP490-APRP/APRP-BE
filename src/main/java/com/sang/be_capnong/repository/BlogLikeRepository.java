package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.BlogLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlogLikeRepository extends JpaRepository<BlogLike, Integer> {

    Optional<BlogLike> findByBlog_BlogIdAndUser_Id(int blogId, int userId);

    long countByBlog_BlogId(int blogId);

    List<BlogLike> findByBlog_BlogIdIn(Collection<Integer> blogIds);

    List<BlogLike> findByBlog_BlogIdInAndUser_Id(Collection<Integer> blogIds, int userId);
}
