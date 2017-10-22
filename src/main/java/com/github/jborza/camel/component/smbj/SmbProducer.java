package com.github.jborza.camel.component.smbj;

import com.hierynomus.smbj.share.File;
import org.apache.camel.Exchange;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.component.file.GenericFileProducer;
import org.apache.camel.util.ExchangeHelper;
import org.apache.camel.util.FileUtil;

public class SmbProducer extends GenericFileProducer<File> {

    private String endpointPath;

    protected SmbProducer(GenericFileEndpoint<File> endpoint, GenericFileOperations<File> operations) {
        super(endpoint, operations);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Exchange smbExchange = getEndpoint().createExchange(exchange);
        setEndpointPath(getEndpoint().getEndpointUri());
        processExchange(smbExchange);
        ExchangeHelper.copyResults(exchange, smbExchange);
    }

    public void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
    }

    protected void processExchange(Exchange exchange) throws Exception {
        String target = createFileName(exchange);
        writeFile(exchange, target);
        // lets store the name we really used in the header, so end-users
        // can retrieve it
        exchange.getIn().setHeader(Exchange.FILE_NAME_PRODUCED, target);
    }

    @Override
    public void writeFile(Exchange exchange, String fileName) throws GenericFileOperationFailedException {
        if (log.isDebugEnabled()) {
            log.debug("writeFile() fileName[" + fileName + "]");
        }
        if(endpoint.isAutoCreate()){
            String name = FileUtil.normalizePath(fileName);
            //TODO windows vs unix

            //strip the share name, as it's a special part of the name for us
            //TODO do this in a nicer place than
            String share = getEndpoint().getConfiguration().getShare();
            name = name.replace(share+"\\","");

            java.io.File file = new java.io.File(name);
            String directory = file.getParent();
            boolean absolute = FileUtil.isAbsolute(file);
            if (directory != null && !operations.buildDirectory(directory, absolute)) {
                log.warn("Cannot build directory [" + directory + "] (could be because of denied permissions)");
            }
        }
        // upload
        if (log.isDebugEnabled()) {
            log.debug("About to write [" + fileName + "] to [" + getEndpoint() + "] from exchange [" + exchange + "]");
        }
        boolean success = operations.storeFile(fileName, exchange);
        if (!success) {
            throw new GenericFileOperationFailedException("Error writing file [" + fileName + "]");
        }
        if (log.isDebugEnabled()) {
            log.debug("Wrote [" + fileName + "] to [" + getEndpoint() + "]");
        }
    }

        @Override
    public SmbEndpoint getEndpoint() {
        return (SmbEndpoint) super.getEndpoint();
    }
}