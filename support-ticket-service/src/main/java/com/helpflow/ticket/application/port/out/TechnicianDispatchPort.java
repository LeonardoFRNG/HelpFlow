package com.helpflow.ticket.application.port.out;

import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;

public interface TechnicianDispatchPort {
    DispatchResult dispatch(Long ticketId, Category category, Priority priority);
}
