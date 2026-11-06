package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.dtos.TagRequest;
import com.roberto.climbingwiki.dtos.TagResponse;
import com.roberto.climbingwiki.exceptions.TagNotFoundException;
import com.roberto.climbingwiki.model.entities.Tag;
import com.roberto.climbingwiki.repositories.TagRepository;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public Page<TagResponse> findAll(Pageable pageable) {
        return tagRepository.findAll(pageable).map(TagResponse::from);
    }

    public TagResponse findById(Long id) {
        return tagRepository.findById(id)
                .map(TagResponse::from)
                .orElseThrow(() -> new TagNotFoundException(Set.of(id)));
    }

    @Transactional
    public TagResponse create(TagRequest request) {
        Tag tag = new Tag();
        tag.setName(request.name());
        return TagResponse.from(tagRepository.save(tag));
    }

    @Transactional
    public void delete(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new TagNotFoundException(Set.of(id));
        }
        tagRepository.deleteById(id);
    }
}
