public enum Piece {
    // 1st Bit = Piece or None?
    None(0),

    // 2nd Bit = Color bit (implies the piece is present)
    Black(1),
    White(3),

    // 3rd+ Bits = Type of Piece
    Pawn(1 << 2),
    Knight(2 << 2),
    Bishop(3 << 2),
    Rook(4 << 2),
    Queen(5 << 2),
    King(6 << 2),

    // White + Type = White Pieces
    WhitePawn(White.value | Pawn.value),
    WhiteKnight(White.value | Knight.value),
    WhiteBishop(White.value | Bishop.value),
    WhiteRook(White.value | Rook.value),
    WhiteQueen(White.value | Queen.value),
    WhiteKing(White.value | King.value),

    // Black + Type = Black Pieces
    BlackPawn(Black.value | Pawn.value),
    BlackKnight(Black.value | Knight.value),
    BlackBishop(Black.value | Bishop.value),
    BlackRook(Black.value | Rook.value),
    BlackQueen(Black.value | Queen.value),
    BlackKing(Black.value | King.value);

    public final int value;
    public static final byte ColorMask = 3,
            TypeMask = 127 - ColorMask;

    Piece(int value) {
        this.value = value;
    }

    public Color Color() {
        return this==Piece.None?null:this.IsWhite() ? Color.White : Color.Black;
    }

    public boolean IsWhite()
    {
        return Pieces.get(value & Piece.ColorMask) == Piece.White;
    }
    public boolean IsBlack()
    {
        return Pieces.get(value & Piece.ColorMask) == Piece.Black;
    }

    //Use Piece.TypeMask to clear the two bits used for color, then set correct color bits
    public Piece OfColor(Color color) {
        return Pieces.get((this.value&Piece.TypeMask) | (color.value+2));
    }

    public boolean IsColor(Piece other)
    {
        return (value&Piece.ColorMask)==(other.value&Piece.ColorMask);
    }
}
