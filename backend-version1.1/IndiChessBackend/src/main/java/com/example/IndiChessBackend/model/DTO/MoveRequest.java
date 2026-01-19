package com.example.IndiChessBackend.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveRequest {

    // Required
    private Integer fromRow;
    private Integer fromCol;
    private Integer toRow;
    private Integer toCol;

    // Optional (for promotion only)
    private String promotedTo;

}
