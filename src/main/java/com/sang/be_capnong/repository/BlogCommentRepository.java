package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.BlogComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BlogCommentRepository extends JpaRepository<BlogComment, Integer> {

    @EntityGraph(attributePaths = {"user"})
    Page<BlogComment> findByBlog_BlogId(int blogId, Pageable pageable);

    long countByBlog_BlogId(int blogId);

    List<BlogComment> findByBlog_BlogIdIn(Collection<Integer> blogIds);
}
