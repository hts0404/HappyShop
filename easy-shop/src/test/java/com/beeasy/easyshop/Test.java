package com.beeasy.easyshop;

import cn.hutool.core.thread.ThreadUtil;
import org.junit.BeforeClass;

import static com.beeasy.web.core.DBService.sqlManager;

public class Test {


    @BeforeClass
    public static void before(){
        ThreadUtil.execAsync(() -> App.main(null));
        ThreadUtil.sleep(3000);
    }

    @org.junit.Test
    public void test() throws Exception {
        //店铺相册表
        sqlManager.genPojoCode("ra_album_class", "com.beeasy.easyshop.model");
        //店铺相册图片表
        sqlManager.genPojoCode("ra_album_pic", "com.beeasy.easyshop.model");
    }
}
