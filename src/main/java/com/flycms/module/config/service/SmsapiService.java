package com.flycms.module.config.service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySendDetailsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySendDetailsResponse;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.flycms.core.entity.DataVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.user.dao.UserDao;
import com.flycms.module.user.model.UserActivation;
import com.flycms.module.config.dao.SmsapiDao;
import com.flycms.module.config.model.Smsapi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 开发公司：28844.com<br/>
 * 版权：28844.com<br/>
 *
 * 短信服务接口服务
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 2018-08-20
 */
@Service
public class SmsapiService {
    @Autowired
    private SmsapiDao smsapiDao;
    @Autowired
    private UserDao userDao;

    //产品名称:云通信短信API产品,开发者无需替换
    static final String product = "Dysmsapi";
    //产品域名,开发者无需替换
    static final String domain = "dysmsapi.aliyuncs.com";

    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    // TODO 此处需要替换成开发者自己的AK(在阿里云访问控制台寻找)

    //发送手机验证码
    public SendSmsResponse sendSms(String PhoneNumber,String code,String accessKeyId,String accessKeySecret,String signName,String templateCode) throws ClientException {
        //可自助调整超时时间
        System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
        System.setProperty("sun.net.client.defaultReadTimeout", "10000");
        //初始化acsClient,暂不支持region化
        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
        DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", product, domain);
        IAcsClient acsClient = new DefaultAcsClient(profile);
        //组装请求对象-具体描述见控制台-文档部分内容
        SendSmsRequest request = new SendSmsRequest();
        //必填:待发送手机号
        request.setPhoneNumbers(PhoneNumber);
        //必填:短信签名-可在短信控制台中找到
        request.setSignName(signName);
        //request.setSignName("猎职网");
        //必填:短信模板-可在短信控制台中找到
        request.setTemplateCode(templateCode);
        //request.setTemplateCode("SMS_126565207");
        //可选:模板中的变量替换JSON串,如模板内容为"亲爱的${name},您的验证码为${code}"时,此处的值为
        request.setTemplateParam("{\"name\":\"Tom\", \"code\":\""+code+"\"}");

        //选填-上行短信扩展码(无特殊需求用户请忽略此字段)
        //request.setSmsUpExtendCode("90997");

        //可选:outId为提供给业务方扩展字段,最终在短信回执消息中将此值带回给调用者
        request.setOutId("用户名");
        //hint 此处可能会抛出异常，注意catch
        SendSmsResponse sendSmsResponse = acsClient.getAcsResponse(request);
        return sendSmsResponse;
    }

    //查询发送回执状态是否成功
    public static QuerySendDetailsResponse querySendDetails(String PhoneNumber,String bizId,String accessKeyId,String accessKeySecret) throws ClientException {

        //可自助调整超时时间
        System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
        System.setProperty("sun.net.client.defaultReadTimeout", "10000");

        //初始化acsClient,暂不支持region化
        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
        DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", product, domain);
        IAcsClient acsClient = new DefaultAcsClient(profile);

        //组装请求对象
        QuerySendDetailsRequest request = new QuerySendDetailsRequest();
        //必填-号码
        request.setPhoneNumber(PhoneNumber);
        //可选-流水号
        request.setBizId(bizId);
        //必填-发送日期 支持30天内记录查询，格式yyyyMMdd
        SimpleDateFormat ft = new SimpleDateFormat("yyyyMMdd");
        request.setSendDate(ft.format(new Date()));
        //必填-页大小
        request.setPageSize(10L);
        //必填-当前页码从1开始计数
        request.setCurrentPage(1L);

        //hint 此处可能会抛出异常，注意catch
        QuerySendDetailsResponse querySendDetailsResponse = acsClient.getAcsResponse(request);

        return querySendDetailsResponse;
    }
    
     /**
     * 手机注册验证码发送并保存
     * 
     * @param PhoneNumber
     *        要发送的手机号码
     * @param code
     *        指定发送的验证码
      * @param type
      *        1手机注册验证码,2安全手机设置验证码,3密码重置验证码
     * @return
     * @throws ClientException
     */
     @Transactional
    public boolean mobileRegisterCode(String PhoneNumber,String code,int type) throws ClientException, InterruptedException{
         Smsapi api=smsapiDao.findSmsapiByid(1);
         String template=null;
         if(type==1){
             template=api.getRegCode();
         }else if(type==2){
             template=api.getSafeCode();
         }else if(type==3){
             template=api.getResetCode();
         }else{
             template=api.getRegCode();
         }
        SendSmsResponse response = sendSms(PhoneNumber,code,api.getAccessKeyId(), api.getAccessKeySecret(),api.getSignName(),template);
        Thread.sleep(3000L);
        //查明细
        if(response.getCode() != null && response.getCode().equals("OK")) {
            QuerySendDetailsResponse querySendDetailsResponse = querySendDetails(PhoneNumber,response.getBizId(),api.getAccessKeyId(), api.getAccessKeySecret());
            int i = 0;
            System.out.println("短信明细查询接口返回数据----------------");
            //查询发送状态
            for(QuerySendDetailsResponse.SmsSendDetailDTO smsSendDetailDTO : querySendDetailsResponse.getSmsSendDetailDTOs())
            {
                System.out.println("PhoneNum=" + smsSendDetailDTO.getPhoneNum());
                //发送成功状态为1则发送成功
                System.out.println("SendStatus=" + smsSendDetailDTO.getSendStatus());
                if(smsSendDetailDTO.getSendStatus()==1) {
                    UserActivation activation=new UserActivation();
                    SnowFlake snowFlake = new SnowFlake(2, 3);
                    activation.setId(snowFlake.nextId());
                    activation.setUserName(PhoneNumber);
                    activation.setCode(code);
                    activation.setCodeType(type);
                    activation.setReferStatus(0);
                    activation.setReferTime(new Date());
                    userDao.addUserActivation(activation);
                    return true;
                }
            }
        }
    	return false;
    }
    // ///////////////////////////////
    // ///// 修改 ////////
    // ///////////////////////////////
    //更新短信api接口信息
    @Transactional
    public DataVo updateSmsapi(Smsapi smsapi){
        DataVo data = DataVo.failure("更新失败");
        int totalCount = smsapiDao.updagteSmsapiById(smsapi);
        if(totalCount > 0){
            data = DataVo.jump("已成功修改","/admin/site/smsapi_edit");
        }
        return data;
    }

    // ///////////////////////////////
    // /////       查询       ////////
    // ///////////////////////////////
    //查询短信接口信息
    public Smsapi findSmsapiByid(Integer id) {
        return smsapiDao.findSmsapiByid(id);
    }
}
