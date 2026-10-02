import com.flycms.module.images.service.ImagesService;

public class LocalizeDebug {
    public static void main(String[] args) throws Exception {
        ImagesService svc = new ImagesService();
        String content = "<p>测试正文 <img src=\"https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png\"> 结束</p>";
        String out = svc.localizeContent(1L, content, "https://img.example.com");
        System.out.println("IN:  " + content);
        System.out.println("OUT: " + out);
    }
}
