/*package com.cvbuilder.controller;

import com.cvbuilder.dto.*;
import com.cvbuilder.service.SkillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/skills")
@Slf4j
public class SkillController {

    @Autowired
    private SkillService skillService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillDTO>>> getAllSkills() {
        try {
            List<SkillDTO> skills = skillService.getAllSkills();
            return ResponseEntity.ok(ApiResponse.success("Tüm beceriler getirildi", skills));
        } catch (Exception e) {
            log.error("Get all skills failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<SkillDTO>>> getSkillsByCategory(@PathVariable String category) {
        try {
            List<SkillDTO> skills = skillService.getSkillsByCategory(category);
            return ResponseEntity.ok(ApiResponse.success("Kategoriye göre beceriler getirildi", skills));
        } catch (Exception e) {
            log.error("Get skills by category failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<SkillDTO>>> getPopularSkills() {
        try {
            List<SkillDTO> skills = skillService.getPopularSkills();
            return ResponseEntity.ok(ApiResponse.success("Popüler beceriler getirildi", skills));
        } catch (Exception e) {
            log.error("Get popular skills failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SkillDTO>>> searchSkills(@RequestParam String keyword) {
        try {
            List<SkillDTO> skills = skillService.searchSkills(keyword);
            return ResponseEntity.ok(ApiResponse.success("Arama sonuçları getirildi", skills));
        } catch (Exception e) {
            log.error("Search skills failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SkillDTO>> createSkill(@Valid @RequestBody SkillDTO skillDTO) {
        try {
            SkillDTO createdSkill = skillService.createSkill(skillDTO);
            return ResponseEntity.ok(ApiResponse.success("Beceri başarıyla oluşturuldu", createdSkill));
        } catch (Exception e) {
            log.error("Create skill failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{skillId}")
    public ResponseEntity<ApiResponse<SkillDTO>> updateSkill(
            @PathVariable Long skillId,
            @Valid @RequestBody SkillDTO skillDTO) {
        try {
            SkillDTO updatedSkill = skillService.updateSkill(skillId, skillDTO);
            return ResponseEntity.ok(ApiResponse.success("Beceri başarıyla güncellendi", updatedSkill));
        } catch (Exception e) {
            log.error("Update skill failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{skillId}")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(@PathVariable Long skillId) {
        try {
            skillService.deleteSkill(skillId);
            return ResponseEntity.ok(ApiResponse.success("Beceri başarıyla silindi", null));
        } catch (Exception e) {
            log.error("Delete skill failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getSkillCategories() {
        try {
            List<String> categories = skillService.getSkillCategories();
            return ResponseEntity.ok(ApiResponse.success("Beceri kategorileri getirildi", categories));
        } catch (Exception e) {
            log.error("Get skill categories failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/high-demand")
    public ResponseEntity<ApiResponse<List<SkillDTO>>> getHighDemandSkills() {
        try {
            List<SkillDTO> skills = skillService.getHighDemandSkills();
            return ResponseEntity.ok(ApiResponse.success("Yüksek talep gören beceriler getirildi", skills));
        } catch (Exception e) {
            log.error("Get high demand skills failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/related")
    public ResponseEntity<ApiResponse<List<SkillDTO>>> getRelatedSkills(
            @RequestParam String category,
            @RequestParam String currentSkill) {
        try {
            List<SkillDTO> skills = skillService.getRelatedSkills(category, currentSkill);
            return ResponseEntity.ok(ApiResponse.success("İlgili beceriler getirildi", skills));
        } catch (Exception e) {
            log.error("Get related skills failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}*/