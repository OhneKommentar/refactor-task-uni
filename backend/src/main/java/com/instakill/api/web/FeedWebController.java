package com.instakill.api.web;

import com.instakill.common.mapping.PostMapper;
import com.instakill.feed.domain.FeedSort;
import com.instakill.infrastructure.security.JwtUserPrincipal;
import com.instakill.interaction.application.InteractionService;
import com.instakill.post.application.PostService;
import com.instakill.user.application.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FeedWebController {

    private final PostService postService;
    private final UserService userService;
    private final InteractionService interactionService;

    public FeedWebController(PostService postService, UserService userService, InteractionService interactionService) {
        this.postService = postService;
        this.userService = userService;
        this.interactionService = interactionService;
    }

    @GetMapping("/feed")
    public String feed(@AuthenticationPrincipal JwtUserPrincipal principal,
                       @RequestParam(defaultValue = "new") String sort,
                       Model model) {
        FeedSort feedSort = FeedSort.from(sort);
        var posts = postService.listFeed(feedSort, 0, 20).stream()
                .map(post -> PostMapper.toResponse(post, userService.getById(post.authorId()),
                        principal != null && interactionService.hasUserLiked(post.id(), principal.id())))
                .toList();
        model.addAttribute("posts", posts);
        model.addAttribute("sort", feedSort.name().toLowerCase());
        return "feed";
    }
}
