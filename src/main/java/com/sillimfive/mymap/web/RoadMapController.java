package com.sillimfive.mymap.web;

import com.sillimfive.mymap.domain.User;
import com.sillimfive.mymap.repository.CategoryRepository;
import com.sillimfive.mymap.service.AwsS3Service;
import com.sillimfive.mymap.service.ImageService;
import com.sillimfive.mymap.service.RoadMapService;
import com.sillimfive.mymap.web.dto.CategoryDto;
import com.sillimfive.mymap.web.dto.roadmap.RoadMapCreateDto;
import com.sillimfive.mymap.web.dto.roadmap.RoadMapEditDto;
import com.sillimfive.mymap.web.dto.roadmap.RoadMapResponseDto;
import com.sillimfive.mymap.web.dto.roadmap.RoadMapSearch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@Tag(name = "RoadMap", description = "road map API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/roadmaps")
public class RoadMapController {

    private final AwsS3Service awsS3Service;
    private final ImageService imageService;
    private final RoadMapService roadMapService;

    private final CategoryRepository categoryRepository;

    @Operation(summary = "로드맵 생성", description = "Create a roadmap (desc)")
    @ApiResponses(value =
        @ApiResponse(
            responseCode = "201", description = "Created", content =
            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema =
            @Schema(example = "1", description = "생성된 로드맵 아이디 반환")))
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public JSONObject create(@RequestBody RoadMapCreateDto roadMapCreateDto, Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        try {
            log.debug("roadMapCreateDto = {}", roadMapCreateDto);

            return roadMapService.create(user.getId(), roadMapCreateDto);
        } catch (Exception e) {
            awsS3Service.delete(roadMapCreateDto.getImageId());

            e.printStackTrace();
            throw new IllegalStateException("Failed to create RoadMap. so delete image for " + roadMapCreateDto.getImageId());
        }
    }

    @Operation(summary = "로드맵 상세 조회", description = "Get roadmap details (desc)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{id}")
    public JSONObject findById(@PathVariable("id") Long roadMapId) {

        return roadMapService.findById(roadMapId);
    }

    @Operation(summary = "로드맵 수정", description = "Edit the roadmap (desc)")
    @PutMapping(value = "/{id}")
    public ResponseEntity<?> edit(
            @PathVariable("id") Long roadMapId,
            @RequestBody RoadMapEditDto roadMapEditDto) {

        return ResponseEntity.ok(roadMapService.edit(roadMapId, roadMapEditDto));
    }

    @Parameter(name = "pageable", hidden = true)
    @Operation(summary = "로드맵 목록 조회", description = "Get roadmap list (desc)",
        parameters = {
            @Parameter(name = "page", example = "0"),
            @Parameter(name = "size", example = "10"),
            @Parameter(name = "sort", example = "id,desc")
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping
    public PageImpl<RoadMapResponseDto> findAll(RoadMapSearch roadMapSearch, Pageable pageable) {
        log.debug("searchCondition: {}", roadMapSearch.toString());

        return roadMapService.findListBy(roadMapSearch, pageable);
    }

    @Operation(summary = "로드맵 카테고리 목록 조회")
    @GetMapping("/categories")
    public List<CategoryDto> categories() {

        return categoryRepository.findAll().stream()
                .map(c -> new CategoryDto(c.getId(), c.getName()))
                .collect(Collectors.toList());
    }


}
