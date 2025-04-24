public class Notation {
    public static char ToChar(Piece piece) {

        return switch (piece) {
            case WhitePawn -> 'P';
            case WhiteKnight -> 'N';
            case WhiteBishop -> 'B';
            case WhiteRook -> 'R';
            case WhiteQueen -> 'Q';
            case WhiteKing -> 'K';
            case BlackPawn -> 'p';
            case BlackKnight -> 'n';
            case BlackBishop -> 'b';
            case BlackRook -> 'r';
            case BlackQueen -> 'q';
            case BlackKing -> 'k';
            default -> ' ';
        };
    }

    public static Piece ToPiece(char ascii) {
        return switch (ascii) {
            case 'P' -> Piece.WhitePawn;
            case 'N' -> Piece.WhiteKnight;
            case 'B' -> Piece.WhiteBishop;
            case 'R' -> Piece.WhiteRook;
            case 'Q' -> Piece.WhiteQueen;
            case 'K' -> Piece.WhiteKing;
            case 'p' -> Piece.BlackPawn;
            case 'n' -> Piece.BlackKnight;
            case 'b' -> Piece.BlackBishop;
            case 'r' -> Piece.BlackRook;
            case 'q' -> Piece.BlackQueen;
            case 'k' -> Piece.BlackKing;
            default -> throw new IllegalArgumentException("Piece character " + ascii + " not supported.");
        };
    }

    public static String ToSquareName(byte squareIndex) {
        //This is the reverse of the ToSquareIndex()
        int rank = squareIndex / 8;
        int file = squareIndex % 8;

        //Map file [0..7] to letters [a..h] and rank [0..7] to [1..8]
        return "" + (char) ('a' + file) + (rank + 1);
    }

    public static byte ToSquare(String squareNotation) {
        //Map letters [a...h] to [0...7] with ASCII('a') == 97
        int file = squareNotation.charAt(0) - 'a';
        //Map numbers [1...8] to [0...7] with ASCII('1') == 49
        int rank = squareNotation.charAt(1) - '1';
        int index = rank * 8 + file;

        if (index >= 0 && index <= 63)
            return (byte) index;

        throw new IllegalArgumentException("The given square notation {squareNotation} does not map to a valid index between 0 and 63");
    }
}