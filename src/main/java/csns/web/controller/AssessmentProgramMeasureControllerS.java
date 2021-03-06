/*
 * This file is part of the CSNetwork Services (CSNS) project.
 * 
 * Copyright 2016, Chengyu Sun (csun@calstatela.edu).
 * 
 * CSNS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * CSNS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with CSNS. If not, see http://www.gnu.org/licenses/agpl.html.
 */
package csns.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import csns.model.academics.Department;
import csns.model.academics.dao.DepartmentDao;
import csns.model.assessment.ProgramMeasure;
import csns.model.assessment.ProgramOutcome;
import csns.model.assessment.Rubric;
import csns.model.assessment.dao.ProgramOutcomeDao;
import csns.model.assessment.dao.RubricDao;
import csns.model.core.Resource;
import csns.model.core.ResourceType;
import csns.model.survey.SurveyChart;
import csns.model.survey.dao.SurveyChartDao;
import csns.security.SecurityUtils;
import csns.util.FileIO;
import csns.web.editor.RubricPropertyEditor;
import csns.web.editor.SurveyChartPropertyEditor;
import csns.web.validator.ProgramMeasureValidator;

@Controller
@SessionAttributes({ "outcome", "measure", "resourceTypes", "rubrics",
    "surveyCharts" })
public class AssessmentProgramMeasureControllerS {

    @Autowired
    private RubricDao rubricDao;

    @Autowired
    private SurveyChartDao surveyChartDao;

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private ProgramOutcomeDao programOutcomeDao;

    @Autowired
    private ProgramMeasureValidator programMeasureValidator;

    @Autowired
    private FileIO fileIO;

    @Autowired
    private WebApplicationContext context;

    private static final Logger logger = LoggerFactory
        .getLogger( AssessmentProgramMeasureControllerS.class );

    @InitBinder
    public void initBinder( WebDataBinder binder )
    {
        binder.registerCustomEditor( Rubric.class,
            (RubricPropertyEditor) context.getBean( "rubricPropertyEditor" ) );
        binder.registerCustomEditor( SurveyChart.class,
            (SurveyChartPropertyEditor) context
                .getBean( "surveyChartPropertyEditor" ) );
    }

    @RequestMapping(
        value = "/department/{dept}/assessment/program/{field}/measure/{operation}",
        method = RequestMethod.GET)
    public String operation( @PathVariable String dept,
        @PathVariable String field, @PathVariable String operation,
        @RequestParam Long fieldId,
        @RequestParam(required = false ) Long measureId, ModelMap models)
    {
        // Do not show the TEXT resource type.
        ResourceType resourceTypes[] = { ResourceType.NONE, ResourceType.FILE,
            ResourceType.URL };
        models.put( "resourceTypes", resourceTypes );

        switch( field )
        {
            case "outcome":
                ProgramOutcome outcome = programOutcomeDao
                    .getProgramOutcome( fieldId );
                ProgramMeasure measure = measureId == null
                    ? new ProgramMeasure() : outcome.getMeasure( measureId );
                models.put( "outcome", outcome );
                models.put( "measure", measure );
                break;

            default:
                logger.warn( "Unsupported program field: " + field );
                return "redirect:../../list";
        }

        Department department = departmentDao.getDepartment( dept );
        models.put( "rubrics", rubricDao.getDepartmentRubrics( department ) );
        models.put( "surveyCharts",
            surveyChartDao.getSurveyCharts( department ) );

        return "assessment/program/measure/" + operation;
    }

    @RequestMapping(
        value = "/department/{dept}/assessment/program/{field}/measure/{operation}",
        method = RequestMethod.POST)
    public String operation( @PathVariable String dept,
        @PathVariable String field, @PathVariable String operation,
        @RequestParam Long fieldId,
        @ModelAttribute("measure" ) ProgramMeasure measure,
        @RequestParam(value = "file",
            required = false) MultipartFile uploadedFile,
        BindingResult result, SessionStatus sessionStatus)
    {
        programMeasureValidator.validate( measure, uploadedFile, result );
        if( result.hasErrors() )
            return "assessment/program/measure/" + operation;

        Resource resource = measure.getResource();
        if( measure.getType().equals( "RUBRIC" ) )
        {
            resource.setType( ResourceType.URL );
            resource.setUrl( "/department/" + dept + "/rubric/results?id="
                + measure.getRubric().getId() );
        }
        else if( measure.getType().equals( "SURVEY" ) )
        {
            resource.setType( ResourceType.URL );
            resource.setUrl( "/department/" + dept + "/survey/chart/view?id="
                + measure.getSurveyChart().getId() );
        }
        else
        {
            if( resource.getType() == ResourceType.FILE ) resource.setFile(
                fileIO.save( uploadedFile, SecurityUtils.getUser(), false ) );
        }

        switch( field )
        {
            case "outcome":
                ProgramOutcome outcome = programOutcomeDao
                    .getProgramOutcome( fieldId );
                outcome.setMeasure( measure );
                outcome = programOutcomeDao.saveProgramOutcome( outcome );
                break;

            default:
                logger.warn( "Unsupported program field: " + field );
        }

        sessionStatus.setComplete();
        logger.info( SecurityUtils.getUser().getUsername() + " " + operation
            + "ed a measure of " + field + " " + fieldId );

        return "redirect:../measures?edit=true&fieldId=" + fieldId;
    }

}
