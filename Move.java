public class Move {
    public final byte FromSquare;
    public final byte ToSquare;
    public final Piece Promotion;

    public Move(int fromSquare, int toSquare) {
        FromSquare = (byte) fromSquare;
        ToSquare = (byte) toSquare;
        Promotion = Piece.None;
    }

    public Move(int fromSquare, int toSquare, Piece promotion) {
        FromSquare = (byte) fromSquare;
        ToSquare = (byte) toSquare;
        Promotion = promotion;
    }

    public Move(String uciMoveNotation) {
        if (uciMoveNotation.length() < 4)
            throw new IllegalArgumentException("Long algebraic notation expected. '{uciMoveNotation}' is too short!");
        if (uciMoveNotation.length() > 5)
            throw new IllegalArgumentException("Long algebraic notation expected. '{uciMoveNotation}' is too long!");

        String fromSquare = uciMoveNotation.substring(0, 2);
        String toSquare = uciMoveNotation.substring(2, 4);
        FromSquare = Notation.ToSquare(fromSquare);
        ToSquare = Notation.ToSquare(toSquare);
        //the presence of a 5th character should mean promotion
        Promotion = (uciMoveNotation.length() == 5) ? Notation.ToPiece(uciMoveNotation.charAt(4)) : Piece.None;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Move move)
            return this.equals(move);

        return false;
    }

    public boolean equals(Move other) {
        return (FromSquare == other.FromSquare) && (ToSquare == other.ToSquare) && (Promotion == other.Promotion);
    }

    @Override
    public int hashCode() {
        //int is big enough to represent move fully. maybe use that for optimization at some point
        return FromSquare + (ToSquare << 8) + (Promotion.value << 16);
    }

    @Override
    public String toString() {
        //the result represents the move in the long algebraic notation (without piece names)
        String result = Notation.ToSquareName(FromSquare);
        result += Notation.ToSquareName(ToSquare);
        //the presence of a 5th character should mean promotion
        if (Promotion != Piece.None)
            result += Notation.ToChar(Promotion);

        return result;
    }

    public static Move BlackCastlingShort = new Move("e8g8");
    public static Move BlackCastlingLong = new Move("e8c8");
    public static Move WhiteCastlingShort = new Move("e1g1");
    public static Move WhiteCastlingLong = new Move("e1c1");

    public static Move BlackCastlingShortRook = new Move("h8f8");
    public static Move BlackCastlingLongRook = new Move("a8d8");
    public static Move WhiteCastlingShortRook = new Move("h1f1");
    public static Move WhiteCastlingLongRook = new Move("a1d1");
}

