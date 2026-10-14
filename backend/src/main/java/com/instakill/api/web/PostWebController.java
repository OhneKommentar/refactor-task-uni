package com.instakill.api.web;

import com.instakill.common.mapping.CommentMapper;
import com.instakill.common.mapping.PostMapper;
import com.instakill.infrastructure.security.JwtUserPrincipal;
import com.instakill.interaction.application.InteractionService;
import com.instakill.post.application.PostService;
import com.instakill.user.application.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/posts")
public class PostWebController {

    private final PostService postService;
    private final UserService userService;
    private final InteractionService interactionService;

    public PostWebController(PostService postService, UserService userService, InteractionService interactionService) {
        this.postService = postService;
        this.userService = userService;
        this.interactionService = interactionService;
    }

    @GetMapping("/new")
    public String newPost(Model model) {
        model.addAttribute("postForm", new PostForm());
        return "post_create";
    }

    @PostMapping
    public String createPost(@AuthenticationPrincipal JwtUserPrincipal principal,
                             @Valid @ModelAttribute("postForm") PostForm form,
                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "post_create";
        }
        postService.createPost(principal.id(), form.getContent(), form.getImageUrl());
        return "redirect:/feed";
    }

    @GetMapping("/{id}")
    public String viewPost(@AuthenticationPrincipal JwtUserPrincipal principal,
                           @PathVariable java.util.UUID id,
                           Model model) {
        populatePostModel(principal, id, model, new CommentForm());
        return "post_detail";
    }

    @PostMapping("/{id}/comments")
    public String addComment(@AuthenticationPrincipal JwtUserPrincipal principal,
                             @PathVariable java.util.UUID id,
                             @Valid @ModelAttribute("commentForm") CommentForm form,
                             BindingResult bindingResult,
                             Model model) {
        if (bindingResult.hasErrors()) {
            populatePostModel(principal, id, model, form);
            return "post_detail";
        }
        interactionService.addComment(id, principal.id(), form.getContent());
        return "redirect:/posts/" + id;
    }

    @PostMapping("/{id}/likes")
    public String toggleLike(@AuthenticationPrincipal JwtUserPrincipal principal,
                              @PathVariable java.util.UUID id) {
        interactionService.toggleLike(id, principal.id());
        return "redirect:/posts/" + id;
    }

    private void populatePostModel(JwtUserPrincipal principal, java.util.UUID id, Model model, CommentForm commentForm) {
        var post = postService.getPost(id);
        var postResponse = PostMapper.toResponse(post, userService.getById(post.authorId()),
                principal != null && interactionService.hasUserLiked(id, principal.id()));
        var comments = interactionService.listComments(id, 0, 50).stream()
                .map(comment -> CommentMapper.toResponse(comment, userService.getById(comment.authorId())))
                .toList();
        model.addAttribute("post", postResponse);
        model.addAttribute("comments", comments);
        model.addAttribute("commentForm", commentForm);
    }
}
