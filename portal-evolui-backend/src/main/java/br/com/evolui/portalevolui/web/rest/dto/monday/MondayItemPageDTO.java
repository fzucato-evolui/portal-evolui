package br.com.evolui.portalevolui.web.rest.dto.monday;

import java.util.List;

public class MondayItemPageDTO {
    private String id;
    private String name;
    private List<MondayColumnValueDTO> column_values;
    private MondayBoardDTO board;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MondayColumnValueDTO> getColumn_values() {
        return column_values;
    }

    public void setColumn_values(List<MondayColumnValueDTO> column_values) {
        this.column_values = column_values;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MondayBoardDTO getBoard() {
        return board;
    }

    public void setBoard(MondayBoardDTO board) {
        this.board = board;
    }
}
