package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Review;
import com.sang.be_capnong.entity.ReviewReaction;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewReactionRepository extends JpaRepository<ReviewReaction, Integer> {
    Optional<ReviewReaction> findByReviewAndBuyer(Review review, User buyer);
    Optional<ReviewReaction> findByReview_ReviewIdAndBuyer_Id(int reviewId, int buyerId);
    long countByReviewAndReactionType(Review review, ReactionType reactionType);
    long countByReview_ReviewIdAndReactionType(int reviewId, ReactionType reactionType);
}
