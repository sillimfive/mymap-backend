package com.sillimfive.mymap.service;

import com.sillimfive.mymap.domain.Category;
import com.sillimfive.mymap.domain.Image;
import com.sillimfive.mymap.domain.roadmap.RoadMap;
import com.sillimfive.mymap.domain.roadmap.RoadMapTag;
import com.sillimfive.mymap.domain.tag.Tag;
import com.sillimfive.mymap.domain.users.User;
import com.sillimfive.mymap.repository.*;
import com.sillimfive.mymap.web.dto.roadmap.*;
import com.sillimfive.mymap.web.dto.study.RoadMapStudyStartDto;
import com.sillimfive.mymap.web.dto.tag.TagDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoadMapService {

    private final RoadMapRepository roadMapRepository;
    private final RoadMapQuerydslRepository roadMapQuerydslRepository;
    private final RoadMapTagRepository roadMapTagRepository;
    private final RoadMapLikeRepository roadMapLikeRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;

    // todo: batch insert for node, tag
    @Transactional
    public Long create(Long userId, RoadMapCreateDto createDto) {

        // find category, tag information
        Optional<Category> category = categoryRepository.findById(createDto.getCategoryId());
        Assert.isTrue(category.isPresent(), "Category should not be null");

        Image image = imageRepository.findById(createDto.getImageId())
                .orElseThrow(() -> new IllegalArgumentException("image is not found for " + createDto.getImageId()));

        List<Tag> tags = new ArrayList<>();
        List<String> tagNames = createDto.getTags();
        if(tagNames.size() != 0) {
            tags.addAll(tagRepository.findByNameIn(tagNames));

            for (String tagName : tagNames) {
                boolean isNewTag = true;
                for (Tag foundTag : tags) {
                    if (foundTag.getName().equals(tagName)) {
                        isNewTag = false;
                        break;
                    }
                }

                if (isNewTag) tags.add(new Tag(tagName));
            }
        }

        // create RoadMapTag
        List<RoadMapTag> roadMapTags = RoadMapTag.createRoadMapTags(tags);

        // create RoadMapNode, RoadMap
        User user = userRepository.findById(userId).get();

        RoadMap roadMap = createDto.convert(user, category.get(), image);
        roadMap.addRoadMapNodes(createDto.getRoadMapNodesFromDto());
        roadMap.addRoadMapTags(roadMapTags);

        roadMapRepository.save(roadMap);

        return roadMap.getId();
    }

    @Transactional
    public Long edit(Long roadMapId, RoadMapEditDto editDto) {
        RoadMap roadMap = roadMapQuerydslRepository.findByIdWithNode(roadMapId)
                .orElseThrow(() -> new IllegalArgumentException("There is no roadMap"));

        if (!roadMap.getImage().getId().equals(editDto.getImageId())) {
            Image image = imageRepository.findById(editDto.getImageId())
                    .orElseThrow(() -> new IllegalArgumentException("주어진 이미지 식별값(id)을 통해 이미지를 찾을 수 없습니다."));

            roadMap.changeImage(image);
        }

        Category foundCategory = categoryRepository.findById(editDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("There is no category searched"));

        if (!roadMap.getCategory().equals(foundCategory)) roadMap.changeCategory(foundCategory);

        boolean contentsChanged = roadMap.changeContents(editDto.getTitle(), editDto.getDescription());
        boolean nodeChanged = roadMap.changeNodeTree(editDto.getNodeDtoList());

        // 제거한 태그 제외
        List<RoadMapTag> deleteList = new ArrayList<>();
        List<String> exceptNames = new ArrayList<>();
        List<String> tagNames = editDto.getTags();

        List<RoadMapTag> roadMapTags = roadMap.getRoadMapTags();
        for (RoadMapTag roadMapTag : roadMapTags) {
            boolean deleteFlag = true;

            for (String tagName : tagNames) {
                if (roadMapTag.getTag().getName().equals(tagName)) {
                    exceptNames.add(tagName);
                    deleteFlag = false;
                    break;
                }
            }

            if (deleteFlag) {
                deleteList.add(roadMapTag);
                roadMapTag.getTag().countDecrease();
            }
        }

        // 변경이 없는 태그들을 제외한 신규 태그들을 신규 태그로 추가한다.
        tagNames.removeAll(exceptNames);
        roadMapTags.addAll(
                RoadMapTag.createRoadMapTags(
                        tagNames.stream()
                                .map(s -> new Tag(s))
                                .collect(Collectors.toList())));
        // todo: add to roadMapHistory

        return roadMapId;
    }

    public RoadMapDetailResponseDto findById(Long id) {
        RoadMap roadMap = roadMapQuerydslRepository.findByIdWithNode(id).orElseThrow(()
                -> new IllegalArgumentException("There is no roadMap for " + id));

        List<TagDto> tags = roadMapTagRepository.findByRoadMapId(id).stream()
                .map(roadMapTag -> new TagDto(roadMapTag.getTag()))
                .collect(Collectors.toList());

        RoadMapDetailResponseDto response = new RoadMapDetailResponseDto(roadMap);
        response.addTags(tags);
        response.setLikeCount(roadMapLikeRepository.getLikeCount(id));

        return response;
    }

    public PageImpl<RoadMapResponseDto> findListBy(RoadMapSearch searchCondition, Pageable pageable) {

        return roadMapQuerydslRepository.searchList(searchCondition, pageable);
    }
}
