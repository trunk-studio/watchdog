@GrabConfig(systemClassLoader=true)
@Grapes([
    @Grab("org.gebish:geb-core:0.9.2"),
    @Grab("org.seleniumhq.selenium:selenium-firefox-driver:2.37.1"),
    @Grab("org.seleniumhq.selenium:selenium-support:2.37.1"),
    @Grab("javax.mail:mail:1.4.7"),
    @Grab("javax.activation:activation"),
    @Grab("ant:ant-javamail:1.6.5")
])
import geb.Browser
import geb.Page
import geb.Module

final smtp_user = System.properties.SMTP_USER
final smtp_pass = System.properties.SMTP_PASS
final isTestOnly = System.properties.TEST_ONLY == 'true'

def lastTime
def output = new StringBuffer()

def status = "GREEN"

def tmpDir = File.createTempFile("kgl",".watchdog")
tmpDir.delete()
tmpDir.mkdirs()

println "Web UI Testing"

Browser.drive {
    config.reportsDir = tmpDir

    output << "Test Google Search: "

    try {
        go "http://google.com/ncr"
    
        lastTime = new Date().time
        
        waitFor { title.startsWith("Google") }
        
        $("input", name: "q").value("amazon web services")
     
        waitFor { title.endsWith("Google Search") }
     
        def firstLink = $("li.g", 0).find("a")
        assert firstLink.text()?.startsWith("Amazon Web Services")
     
        firstLink.click()
     
        waitFor { title.startsWith("Amazon Web Services") }
    
        output << "${new Date().time - lastTime}ms\n"
    }
    catch (e) {
        status = "RED"
        output << "timeout\n"
    }
    finally {
        report "google_search_amazon_web_services"
    }
    
    output << "Test Koobe B2B: "

    try {
        go "http://koobe.koobelibrary.com/"
        
        lastTime = new Date().time
        
        waitFor { title.startsWith("KGL B2B Service") && $("input").size() >= 2 }
        
        $("input").value("admin")
        
        $("button.signin").click();
        
        waitFor { $("img.gwt-Image").size() >= 21 }
        
        output << "${new Date().time - lastTime}ms\n"
    }
    catch (e) {
        status = "RED"
        output << "timeout\n"
    }
    finally {
        report "kgl_b2b"
    }
    
    output << "Test Koobe Forestlib: "

    try {
        go "http://embed.forestlib.com/"
        
        lastTime = new Date().time
        
        waitFor { title.contains("Global Viewer") }
        
        output << "${new Date().time - lastTime}ms\n"
    }
    catch (e) {
        status = "RED"
        output << "timeout\n"
    }
    finally {
        report "kgl_forestlib"
    }

}.quit()

println output

println "Executing Commands"

def isWindows = System.properties['os.name'].toLowerCase().contains('windows')

def tracerouteCmd = isWindows?'tracert':'traceroute'

def exec = { command ->
    def stream = new ByteArrayOutputStream()
    def proc = command.execute()
    proc.consumeProcessOutput(stream, System.err)
    proc.waitForOrKill(60000)
    stream.toString()
}

def filename = {
    it.replaceAll(/[\s-]+/, '_')+".txt"
}

def cmds = []
//cmds << "uname -a"
cmds << "${tracerouteCmd} koobe.koobelibrary.com"
cmds << "${tracerouteCmd} embed.forestlib.com"

cmds.each { cmd ->
     def file1 = new File(tmpDir, filename(cmd))
     file1 << "\n\n===== ${cmd} =====\n${exec(cmd)}\n"
}

def opts = [
    mailhost: "smtp.gmail.com",
    mailport: 465,
    ssl: true,
    enableStartTLS: "true",
    user: smtp_user,
    password: smtp_pass,
    messagemimetype: "text/plain",
    subject: "[${status}] KGL Watchdog"
]

def ant = new AntBuilder()
ant.mail(opts) {
    from(address: "kyle@koobe.com.tw")
    to(address: "kyle@koobe.com.tw")
    if (!isTestOnly) {
        cc(address: "arthur@kdhservice.com")
        cc(address: "ann@koobe.com.tw")
        cc(address: "thomas.l.c.tarn@gmail.com")
        cc(address: "cloude@koobe.com.tw")
    }
    message(output.toString())
    attachments(){
        fileset(dir: tmpDir){
            include(name: "*.txt")
            include(name: "*.png")
        }
    }
}
