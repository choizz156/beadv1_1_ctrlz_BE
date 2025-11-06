package com.userservice.infrastructure.reader.port;

import com.userservice.infrastructure.reader.port.dto.UserDescription;

public interface UserInformationReader {
	UserDescription getUserDescription(String id);
}
