import com.google.common.collect.Maps;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import	java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Project : sunlands-activiti
 * @Package Name : com.sunlands.feo.workflow.controller
 * @Description : TODO
 * @Author : eleven
 * @Create Date : 2019年08月21日 10:36
 * @ModificationHistory Who   When     What
 * ------------    --------------    ---------------------------------
 */

public class ActivitiMain {

    private static final Logger logger = LoggerFactory.getLogger(ActivitiMain.class);

    public static void main(String[] args) throws ParseException {
        logger.info("程序启动--------------");

        //创建流程引擎
        ProcessEngine processEngine = getProcessEngine();

        //部署流程定义文件
        ProcessDefinition processDefinition = getProcessDefinition(processEngine,"OfferApproveSales.bpmn20.xml");

        //启动运行流程
        ProcessInstance processInstance = getProcessInstance(processEngine, processDefinition);

        //处理流程任务
        processTask(processEngine, processInstance);

        logger.info("程序结束--------------");

    }


    /**
     * 执行任务
     * @param processEngine
     * @param processInstance
     * @throws ParseException
     */
    private static void processTask(ProcessEngine processEngine, ProcessInstance processInstance) throws ParseException {
        Scanner scanner = new Scanner(System.in);
        while(processInstance != null && !processInstance.isEnded()){
            //获取处理任务
            TaskService taskService = processEngine.getTaskService();
            List<Task> list = taskService.createTaskQuery().list();
            logger.info("待处理任务数：[{}]", list.size());
            for (Task task : list) {
                logger.info("待处理任务：[{}]", task.getName());
                //获取变量
                Map<String, Object> variables = getVariablesMap(processEngine, scanner, task);

                //提交表单
                taskService.complete(task.getId(), variables);

                //获取当前流程实例最新的状态
                processInstance = processEngine.getRuntimeService()
                        .createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .singleResult();
            }
        }
        scanner.close();
    }


    /**
     * 获取变量
     * @param processEngine
     * @param scanner
     * @param task
     * @return
     * @throws ParseException
     */
    private static Map<String, Object> getVariablesMap(ProcessEngine processEngine, Scanner scanner, Task task) throws ParseException {

        //获取表单
        FormService formService = processEngine.getFormService();
        TaskFormData taskFormData = formService.getTaskFormData(task.getId());
        //获取属性集合
        List<FormProperty> formProperties = taskFormData.getFormProperties();

        Map<String,Object> variables = Maps.newHashMap();

        for (FormProperty formProperty : formProperties) {
            String line = null;
            if(StringFormType.class.isInstance(formProperty.getType())){
                logger.info("请输入 [{}]",formProperty.getName());
                line = scanner.nextLine();
                variables.put(formProperty.getId(),line);
            }else if(DateFormType.class.isInstance(formProperty.getType())){
                logger.info("请输入 [{}] ？格式（yyyy-MM-dd）",formProperty.getName());
                line = scanner.nextLine();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = simpleDateFormat.parse(line);
                variables.put(formProperty.getId(),date);
            }else{
                logger.info("类型暂时不支持：[{}]",formProperty.getType());
            }
            logger.info("您输入的内容是: [{}]", line);
        }
        return variables;
    }

    /**
     * 启动运行流程
     * @param processEngine
     * @param processDefinition
     * @return
     */
    private static ProcessInstance getProcessInstance(ProcessEngine processEngine, ProcessDefinition processDefinition) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        logger.info("启动流程：[{}]",processInstance.getProcessDefinitionKey());
        return processInstance;

    }


    /**
     * 部署流程定义文件
     * @param processEngine
     * @return
     */
    private static ProcessDefinition getProcessDefinition(ProcessEngine processEngine,String bpmnFile) {
        //获取service,对流程定义文件进行操作
        RepositoryService repositoryService = processEngine.getRepositoryService();
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource(bpmnFile);
        Deployment deployment = deploymentBuilder.deploy();
        String id = deployment.getId();

        //获取流程定义对象
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(id).singleResult();
        String processFileId = processDefinition.getId();
        String processFileIdName = processDefinition.getName();
        logger.info("流程定义文件:[{}],流程id:[{}]",processFileIdName,processFileId);
        return processDefinition;
    }


    /**
     * 创建流程引擎
     * @return
     */
    private static ProcessEngine getProcessEngine() {
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = cfg.buildProcessEngine();
        String name = processEngine.getName();
        String version = ProcessEngine.VERSION;
        logger.info("流程引擎名称：[{}],版本：[{}]",name,version);
        return processEngine;
    }


}
