package com.alumni.academic_management_api.repository.specification;

import com.alumni.academic_management_api.entity.Job;
import com.alumni.academic_management_api.enums.ExperienceLevel;
import com.alumni.academic_management_api.enums.WorkplaceType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

public class JobSpecification {

    private JobSpecification() {
    }

    public static Specification<Job> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Job> hasKeyword(String keyword) {
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("company")), pattern)
        );
    }

    public static Specification<Job> hasArea(String area) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("area")), "%" + area.toLowerCase() + "%");
    }

    public static Specification<Job> hasExperienceLevels(List<ExperienceLevel> levels) {
        return (root, query, cb) -> root.get("experienceLevel").in(levels);
    }

    public static Specification<Job> hasLocation(String location) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
    }

    public static Specification<Job> hasSalaryGreaterOrEqual(BigDecimal minSalary) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("salary"), minSalary);
    }

    public static Specification<Job> isRemote() {
        return (root, query, cb) -> cb.equal(root.get("workplaceType"), WorkplaceType.REMOTE);
    }
}