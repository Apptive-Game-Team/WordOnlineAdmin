package com.wordonline.admin.controller;

import com.wordonline.admin.dto.tag.TagDto;
import com.wordonline.admin.dto.tag.TagRequestDto;
import com.wordonline.admin.service.TagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
@AutoConfigureMockMvc(addFilters = false)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagService tagService;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void createAnswersConflictWithTheReadableMessage() throws Exception {
        when(tagService.addTag(new TagRequestDto("CAT_AoE")))
                .thenThrow(new IllegalArgumentException("Tag name already exists: CAT_AoE"));

        mockMvc.perform(post("/api/admin/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"CAT_AoE\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Tag name already exists: CAT_AoE"));
    }

    @Test
    void renameAnswersConflictWithTheReadableMessage() throws Exception {
        doThrow(new IllegalArgumentException("Tag name already exists: CAT_AoE"))
                .when(tagService).putTag(new TagDto(3L, "CAT_AoE"));

        mockMvc.perform(put("/api/admin/tags/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"CAT_AoE\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Tag name already exists: CAT_AoE"));
    }
}
