package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.BlogComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogCommentRepository extends JpaRepository<BlogComment, Integer> {

    Page<BlogComment> findByBlog_BlogId(int blogId, Pageable pageable);

    long countByBlog_BlogId(int blogId);
}
