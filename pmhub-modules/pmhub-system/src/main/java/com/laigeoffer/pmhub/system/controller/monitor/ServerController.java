package com.laigeoffer.pmhub.system.controller.monitor;

import com.laigeoffer.pmhub.base.core.core.domain.AjaxResult;
import com.laigeoffer.pmhub.base.core.core.domain.Server;
import com.laigeoffer.pmhub.base.security.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// ... existing code ...

@RestController
@RequestMapping("/system/monitor/server")
public class ServerController {
    @RequiresPermissions("monitor:server:list")
    @GetMapping()
    public AjaxResult getInfo() throws Exception {
        Server server = new Server();
        server.copyTo();
        return AjaxResult.success(server);
    }
}
