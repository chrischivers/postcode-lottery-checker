package com.ditcherj.contextio;

import com.ditcherj.contextio.responses.BaseResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.model.Response;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by Jonathan Ditcher on 01/04/2017.
 */
public class ResponseBuilder {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ResponseBuilder.class);

    private Response response;
    private Integer code;

    public ResponseBuilder(Response response) {
        this.response = response;
        this.code = response.getCode();
    }

    public<T extends BaseResponse> T decodeResponse(Class<T> clazz) {
        ObjectMapper objectMapper = new ObjectMapper();
        T obj = null;
        try {
            String content = this.response.getBody();
            obj = objectMapper.readValue(content, clazz);
            obj.setCode(this.code);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            logger.warn("response[{}]", this.response);
        }

        if(obj == null) {
            try {
                obj = clazz.newInstance();
                obj.setCode(this.code);
                obj.setError(this.response.getMessage());
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return obj;
    }

    public<T> List<T> decodeResponseAsList(TypeReference type) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<T> list = null;
        try {
            list = objectMapper.readValue(this.response.getBody(), type);
            for(T obj : list) {
                if(obj instanceof BaseResponse)
                    ((BaseResponse)obj).setCode(this.code);
            }
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
        return list;
    }

}
