package com.example.dingding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dingding.entity.FormComponentValue;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 表单组件值Mapper接口
 *
 * @author system
 * @version 1.0.0
 */
@Mapper
public interface FormComponentValueMapper extends BaseMapper<FormComponentValue> {

    /**
     * 根据流程实例ID和组件名称查询
     */
    @Select("SELECT * FROM ding_form_component_values " +
            "WHERE process_instance_id = #{processInstanceId} AND component_name = #{componentName}")
    FormComponentValue selectByProcessInstanceAndComponentName(@Param("processInstanceId") String processInstanceId,
                                                              @Param("componentName") String componentName);

    /**
     * 根据流程实例ID列表删除记录
     */
    @Delete("<script>" +
            "DELETE FROM ding_form_component_values " +
            "WHERE process_instance_id IN " +
            "<foreach collection='processInstanceIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteByProcessInstanceIds(@Param("processInstanceIds") List<String> processInstanceIds);

    /**
     * 根据流程实例ID列表批量查询表单组件值
     */
    @Select("<script>" +
            "SELECT * FROM ding_form_component_values " +
            "WHERE process_instance_id IN " +
            "<foreach collection='processInstanceIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND component_name = #{componentName}" +
            "</script>")
    List<FormComponentValue> selectByProcessInstanceIdsAndComponentName(@Param("processInstanceIds") List<String> processInstanceIds,
                                                                        @Param("componentName") String componentName);

    /**
     * 统计改善等级分布
     */
    @Select("SELECT fcv.value as improvementLevel, COUNT(*) as count " +
            "FROM ding_form_component_values fcv " +
            "INNER JOIN ding_process_instances dpi ON fcv.process_instance_id = dpi.process_instance_id " +
            "WHERE fcv.component_name = '改善等级' " +
            "AND dpi.create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY fcv.value")
    List<ImprovementLevelStats> selectImprovementLevelStats(@Param("startTime") String startTime,
                                                           @Param("endTime") String endTime);

    /**
     * 统计提案类别分布
     */
    @Select("SELECT fcv.value as proposalCategory, COUNT(*) as count " +
            "FROM ding_form_component_values fcv " +
            "INNER JOIN ding_process_instances dpi ON fcv.process_instance_id = dpi.process_instance_id " +
            "WHERE fcv.component_name = '提案类别' " +
            "AND dpi.create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY fcv.value")
    List<ProposalCategoryStats> selectProposalCategoryStats(@Param("startTime") String startTime,
                                                            @Param("endTime") String endTime);

    /**
     * 统计经济效益
     */
    @Select("SELECT dpi.originator_userid, " +
            "SUM(CAST(CASE WHEN fcv.value != '' AND fcv.value IS NOT NULL THEN fcv.value ELSE '0' END AS DECIMAL(10,2))) as totalBenefit " +
            "FROM ding_process_instances dpi " +
            "LEFT JOIN ding_form_component_values fcv ON dpi.process_instance_id = fcv.process_instance_id " +
            "AND fcv.component_name = '预计产生经济效益(元)' " +
            "WHERE dpi.create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY dpi.originator_userid")
    List<EconomicBenefitStats> selectEconomicBenefitStats(@Param("startTime") String startTime,
                                                         @Param("endTime") String endTime);

    /**
     * 改善等级统计结果类
     */
    class ImprovementLevelStats {
        private String improvementLevel;
        private Long count;

        public String getImprovementLevel() { return improvementLevel; }
        public void setImprovementLevel(String improvementLevel) { this.improvementLevel = improvementLevel; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }

    /**
     * 提案类别统计结果类
     */
    class ProposalCategoryStats {
        private String proposalCategory;
        private Long count;

        public String getProposalCategory() { return proposalCategory; }
        public void setProposalCategory(String proposalCategory) { this.proposalCategory = proposalCategory; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }

    /**
     * 经济效益统计结果类
     */
    class EconomicBenefitStats {
        private String originatorUserid;
        private Double totalBenefit;

        public String getOriginatorUserid() { return originatorUserid; }
        public void setOriginatorUserid(String originatorUserid) { this.originatorUserid = originatorUserid; }
        public Double getTotalBenefit() { return totalBenefit; }
        public void setTotalBenefit(Double totalBenefit) { this.totalBenefit = totalBenefit; }
    }
}