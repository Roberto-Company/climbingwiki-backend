package com.roberto.climbingwiki.project1.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.project1.dtos.TagRequest;
import com.roberto.climbingwiki.project1.dtos.TagResponse;
import com.roberto.climbingwiki.project1.exceptions.TagNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.Tag;
import com.roberto.climbingwiki.project1.repositories.TagRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    private TagService tagService;

    private Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }

    @BeforeEach
    void setUp() {
        tagService = new TagService(tagRepository);
    }

    @Test
    void findAll_returnsMappedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Tag> tagPage = new PageImpl<>(List.of(tag(1L, "classic"), tag(2L, "crimpy")), pageable, 2);
        when(tagRepository.findAll(pageable)).thenReturn(tagPage);

        Page<TagResponse> result = tagService.findAll(pageable);

        assertThat(result.getContent()).extracting(TagResponse::name).containsExactly("classic", "crimpy");
    }

    @Test
    void findById_existingTag_returnsResponse() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag(1L, "classic")));

        TagResponse result = tagService.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("classic");
    }

    @Test
    void findById_missingTag_throws() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.findById(99L))
                .isInstanceOf(TagNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_savesAndReturnsResponse() {
        when(tagRepository.save(any(Tag.class))).thenReturn(tag(5L, "overhang"));

        TagResponse result = tagService.create(new TagRequest("overhang"));

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.name()).isEqualTo("overhang");
    }

    @Test
    void delete_existingTag_deletes() {
        when(tagRepository.existsById(1L)).thenReturn(true);

        tagService.delete(1L);

        verify(tagRepository).deleteById(1L);
    }

    @Test
    void delete_missingTag_throwsAndDoesNotDelete() {
        when(tagRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> tagService.delete(99L)).isInstanceOf(TagNotFoundException.class);

        verify(tagRepository, never()).deleteById(99L);
    }
}
