package com.zhuwenjin;


import com.zhuwenjin.annotation.Service;

/**
 * @author zhuwenjin
 */
@Service
public class ByeServiceImpl implements ByeService {

    @Override
    public String bye(String name) {
        return "bye, " + name;
    }
}
