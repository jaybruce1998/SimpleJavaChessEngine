//    A  B  C  D  E  F  G  H        BLACK
// 8  56 57 58 59 60 61 62 63  8
// 7  48 49 50 51 52 53 54 55  7
// 6  40 41 42 43 44 45 46 47  6
// 5  32 33 34 35 36 37 38 39  5
// 4  24 25 26 27 28 29 30 31  4
// 3  16 17 18 19 20 21 22 23  3
// 2  08 09 10 11 12 13 14 15  2
// 1  00 01 02 03 04 05 06 07  1
//    A  B  C  D  E  F  G  H        WHITE

import java.util.function.Consumer;

public class Board {
    static final int BlackKingSquare = Notation.ToSquare("e8");
    static final int WhiteKingSquare = Notation.ToSquare("e1");
    static final int BlackQueensideRookSquare = Notation.ToSquare("a8");
    static final int BlackKingsideRookSquare = Notation.ToSquare("h8");
    static final int WhiteQueensideRookSquare = Notation.ToSquare("a1");
    static final int WhiteKingsideRookSquare = Notation.ToSquare("h1");

    public static final String STARTING_POS_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    static class CastlingRights {
        public static final byte None = 0,
                WhiteKingside = 1,
                WhiteQueenside = 2,
                BlackKingside = 4,
                BlackQueenside = 8,
                All = 15;
    }

    /*** STATE DATA ***/
    private final Piece[] _state = new Piece[64];
    private byte _castlingRights = CastlingRights.All;
    private Color _sideToMove = Color.White;
    private int _enPassantSquare = -1;
    private long _zobristHash = 0;
    private Evaluation.Eval _eval;
    /*** STATE DATA ***/

    public int Score() {
        return _eval.Score();
    }

    public long getZobristHash() {
        return _zobristHash;
    }

    private void setSideToMove(Color value) {
        _zobristHash ^= Zobrist.SideToMove(_sideToMove);
        _sideToMove = value;
        _zobristHash ^= Zobrist.SideToMove(_sideToMove);
    }

    public Color getSideToMove() {
        return _sideToMove;
    }

    public Board() {
    }

    public Board(String fen) {
        SetupPosition(fen);
    }

    public Board(Board board) {
        Copy(board);
    }

    public Board(Board board, Move move) {
        Copy(board);
        Play(move);
    }

    public void Copy(Board board) {
        System.arraycopy(board._state, 0, _state, 0, 64);
        _sideToMove = board._sideToMove;
        _enPassantSquare = board._enPassantSquare;
        _castlingRights = board._castlingRights;
        _zobristHash = board._zobristHash;
        _eval = new Evaluation.Eval(board._eval);//MAKE A COPY OF THIS OMG!
    }

    public Piece get(int index) {
        return _state[index];
    }

    //Rank - the eight horizontal rows of the chess board are called ranks.
    //File - the eight vertical columns of the chess board are called files.
    public Piece get(int rank, int file) {
        return _state[rank * 8 + file];
    }

    private void set(int square, Piece value) {
        _eval.Update(_state[square], value, square);
        _zobristHash ^= Zobrist.PieceSquare(_state[square], square);
        _state[square] = value;
        _zobristHash ^= Zobrist.PieceSquare(_state[square], square);
    }

    public void SetupPosition(String fen) {
        String[] fields = fen.split(" ");
        if (fields.length < 4)
            throw new IllegalArgumentException("FEN needs at least 4 fields. Has only {fields.Length} fields.");

        // Place pieces on board.
        for (int i = 0; i < 64; i++)
            _state[i] = Piece.None;//refactor?
        String[] fenPosition = fields[0].split("/");
        int rank = 7;
        for (String row : fenPosition) {
            int file = 0;
            for (char piece : row.toCharArray()) {
                if (Character.isDigit(piece)) {
                    file += piece - '0';
                } else {
                    _state[rank * 8 + file] = Notation.ToPiece(piece);
                    file++;
                }
            }
            rank--;
        }

        // Set side to move.
        _sideToMove = fields[1].equals("w") ? Color.White : Color.Black;

        // Set castling rights.
        SetCastlingRights(CastlingRights.WhiteKingside, fields[2].contains("K"));
        SetCastlingRights(CastlingRights.WhiteQueenside, fields[2].contains("Q"));
        SetCastlingRights(CastlingRights.BlackKingside, fields[2].contains("k"));
        SetCastlingRights(CastlingRights.BlackQueenside, fields[2].contains("q"));

        // Set en passant square.
        _enPassantSquare = fields[3].equals("-") ? -1 : Notation.ToSquare(fields[3]);
        _eval = new Evaluation.Eval(this);

        //Initialze Hash
        InitZobristHash();
    }

    //*****************
    //** PLAY MOVES ***
    //*****************

    public void PlayNullMove() {
        setSideToMove(Pieces.Flip(_sideToMove));
        //Clear en passant
        _zobristHash ^= Zobrist.EnPassant(_enPassantSquare);
        _enPassantSquare = -1;
    }

    public void Play(Move move) {
        Piece movingPiece = _state[move.FromSquare];
        if (move.Promotion != Piece.None)
            movingPiece = move.Promotion.OfColor(_sideToMove);

        //move the correct piece to the target square
        set(move.ToSquare, movingPiece);
        //...and clear the square it was previously located
        set(move.FromSquare, Piece.None);

        int[] captureIndex = new int[1];
        if (IsEnPassant(movingPiece, move, captureIndex)) {
            //capture the pawn
            set(captureIndex[0], Piece.None);
        }

        //handle castling special case
        Move[] rm = new Move[1];
        if (IsCastling(movingPiece, move, rm)) {
            //move the rook to the target square and clear from square
            Move rookMove = rm[0];
            set(rookMove.ToSquare, _state[rookMove.FromSquare]);
            set(rookMove.FromSquare, Piece.None);
        }

        //update board state
        UpdateEnPassant(move);
        UpdateCastlingRights(move.FromSquare);
        UpdateCastlingRights(move.ToSquare);

        //toggle active color!
        setSideToMove(Pieces.Flip(_sideToMove));
    }

    private void UpdateCastlingRights(int squareIndex) {
        //any move from or to king or rook squares will affect castling rights
        if (squareIndex == WhiteKingSquare || squareIndex == WhiteQueensideRookSquare)
            SetCastlingRights(CastlingRights.WhiteQueenside, false);
        if (squareIndex == WhiteKingSquare || squareIndex == WhiteKingsideRookSquare)
            SetCastlingRights(CastlingRights.WhiteKingside, false);

        if (squareIndex == BlackKingSquare || squareIndex == BlackQueensideRookSquare)
            SetCastlingRights(CastlingRights.BlackQueenside, false);
        if (squareIndex == BlackKingSquare || squareIndex == BlackKingsideRookSquare)
            SetCastlingRights(CastlingRights.BlackKingside, false);
    }

    private void UpdateEnPassant(Move move) {
        _zobristHash ^= Zobrist.EnPassant(_enPassantSquare);
        int to = move.ToSquare;
        int from = move.FromSquare;
        Piece movingPiece = _state[to];

        //movingPiece needs to be either a BlackPawn...
        if (movingPiece == Piece.BlackPawn && Rank(to) == Rank(from) - 2)
            _enPassantSquare = down(from);
        else if (movingPiece == Piece.WhitePawn && Rank(to) == Rank(from) + 2)
            _enPassantSquare = up(from);
        else
            _enPassantSquare = -1;
        _zobristHash ^= Zobrist.EnPassant(_enPassantSquare);
    }

    private boolean IsEnPassant(Piece movingPiece, Move move, int[] captureIndex) {
        if (movingPiece == Piece.BlackPawn && move.ToSquare == _enPassantSquare) {
            captureIndex[0] = up(_enPassantSquare);
            return true;
        } else if (movingPiece == Piece.WhitePawn && move.ToSquare == _enPassantSquare) {
            captureIndex[0] = down(_enPassantSquare);
            return true;
        }

        //not en passant
        captureIndex[0] = -1;
        return false;
    }

    private boolean IsCastling(Piece moving, Move move, Move[] rookMove) {
        if (moving == Piece.BlackKing && move.equals(Move.BlackCastlingLong)) {
            rookMove[0] = Move.BlackCastlingLongRook;
            return true;
        }
        if (moving == Piece.BlackKing && move.equals(Move.BlackCastlingShort)) {
            rookMove[0] = Move.BlackCastlingShortRook;
            return true;
        }
        if (moving == Piece.WhiteKing && move.equals(Move.WhiteCastlingLong)) {
            rookMove[0] = Move.WhiteCastlingLongRook;
            return true;
        }
        if (moving == Piece.WhiteKing && move.equals(Move.WhiteCastlingShort)) {
            rookMove[0] = Move.WhiteCastlingShortRook;
            return true;
        }

        //not castling
        rookMove[0] = null;
        return false;
    }

    //**********************
    //** MOVE GENERATION ***
    //**********************

    public boolean IsPlayable(Move move) {
        if (move == null)
            return false;
        final boolean[] found = {false};
        CollectMoves(move.FromSquare, m -> found[0] |= (m.equals(move)));
        return found[0];
    }

    public void CollectMoves(Consumer<Move> moveHandler) {
        for (int square = 0; square < 64; square++)
            CollectMoves(square, moveHandler);
    }

    public void CollectQuiets(Consumer<Move> moveHandler) {
        for (int square = 0; square < 64; square++)
            CollectQuiets(square, moveHandler);
    }

    public void CollectCaptures(Consumer<Move> moveHandler) {
        for (int square = 0; square < 64; square++)
            CollectCaptures(square, moveHandler);
    }

    public void CollectMoves(int square, Consumer<Move> moveHandler) {
        CollectQuiets(square, moveHandler);
        CollectCaptures(square, moveHandler);
    }

    public void CollectQuiets(int square, Consumer<Move> moveHandler) {
        if (IsActivePiece(_state[square]))
            AddQuiets(square, moveHandler);
    }

    public void CollectCaptures(int square, Consumer<Move> moveHandler) {
        if (IsActivePiece(_state[square]))
            AddCaptures(square, moveHandler);
    }

    private void AddQuiets(int square, Consumer<Move> moveHandler) {
        switch (_state[square]) {
            case Piece.BlackPawn:
                AddBlackPawnMoves(moveHandler, square);
                break;
            case Piece.WhitePawn:
                AddWhitePawnMoves(moveHandler, square);
                break;
            case Piece.BlackKing:
                AddBlackCastlingMoves(moveHandler);
                AddMoves(moveHandler, square, Attacks.King);
                break;
            case Piece.WhiteKing:
                AddWhiteCastlingMoves(moveHandler);
                AddMoves(moveHandler, square, Attacks.King);
                break;
            case Piece.BlackKnight:
            case Piece.WhiteKnight:
                AddMoves(moveHandler, square, Attacks.Knight);
                break;
            case Piece.BlackRook:
            case Piece.WhiteRook:
                AddMoves(moveHandler, square, Attacks.Rook);
                break;
            case Piece.BlackBishop:
            case Piece.WhiteBishop:
                AddMoves(moveHandler, square, Attacks.Bishop);
                break;
            case Piece.BlackQueen:
            case Piece.WhiteQueen:
                AddMoves(moveHandler, square, Attacks.Queen);
                break;
        }
    }

    private void AddCaptures(int square, Consumer<Move> moveHandler) {
        switch (_state[square]) {
            case Piece.BlackPawn:
                AddBlackPawnAttacks(moveHandler, square);
                break;
            case Piece.WhitePawn:
                AddWhitePawnAttacks(moveHandler, square);
                break;
            case Piece.BlackKing:
            case Piece.WhiteKing:
                AddCaptures(moveHandler, square, Attacks.King);
                break;
            case Piece.BlackKnight:
            case Piece.WhiteKnight:
                AddCaptures(moveHandler, square, Attacks.Knight);
                break;
            case Piece.BlackRook:
            case Piece.WhiteRook:
                AddCaptures(moveHandler, square, Attacks.Rook);
                break;
            case Piece.BlackBishop:
            case Piece.WhiteBishop:
                AddCaptures(moveHandler, square, Attacks.Bishop);
                break;
            case Piece.BlackQueen:
            case Piece.WhiteQueen:
                AddCaptures(moveHandler, square, Attacks.Queen);
                break;
        }
    }

    private void AddMove(Consumer<Move> moveHandler, int from, int to) {
        moveHandler.accept(new Move(from, to));
    }

    private void AddPromotion(Consumer<Move> moveHandler, int from, int to, Piece promotion) {
        moveHandler.accept(new Move(from, to, promotion));
    }

    //*****************
    //** CHECK TEST ***
    //*****************

    public boolean IsChecked(Color color)
    {
        Piece king = Piece.King.OfColor(color);
        for (int square = 0; square < 64; square++)
            if(_state[square] == king)
                return IsSquareAttackedBy(square, Pieces.Flip(color));

        throw new RuntimeException("No {color} King found!");
    }

    private boolean IsSquareAttackedBy(int square, Color color)
    {
        //1. Pawns? (if attacker is white, pawns move up and the square is attacked from below. squares below == Attacks.BlackPawn)
        var pawnAttacks = color == Color.White ? Attacks.BlackPawn : Attacks.WhitePawn;
        for (int target : pawnAttacks[square])
            if (_state[target] == Piece.Pawn.OfColor(color))
                return true;

        //2. Knight
        for (int target : Attacks.Knight[square])
            if (_state[target] == Piece.Knight.OfColor(color))
                return true;

        //3. Queen or Bishops on diagonals lines
        for (int dir = 0; dir < 4; dir++)
            for (int target : Attacks.Bishop[square][dir]) {
                if (_state[target] == Piece.Bishop.OfColor(color) || _state[target] == Piece.Queen.OfColor(color))
                    return true;
                if (_state[target] != Piece.None)
                    break;
            }

        //4. Queen or Rook on straight lines
        for (int dir = 0; dir < 4; dir++)
            for (int target : Attacks.Rook[square][dir]) {
                if (_state[target] == Piece.Rook.OfColor(color) || _state[target] == Piece.Queen.OfColor(color))
                    return true;
                if (_state[target] != Piece.None)
                    break;
            }

        //5. King
        for (int target : Attacks.King[square])
            if (_state[target] == Piece.King.OfColor(color))
                return true;

        return false; //not threatened by anyone!
    }


    //****************
    //** CAPTURES **
    //****************

    private void AddCaptures(Consumer<Move> moveHandler, int square, byte[][] targets) {
        for (int target : targets[square])
            if (IsOpponentPiece(_state[target]))
                AddMove(moveHandler, square, target);
    }

    private void AddCaptures(Consumer<Move> moveHandler, int square, byte[][][] targets) {
        for (var axis : targets[square])
            for (int target : axis)
                if (_state[target] != Piece.None) {
                    if (IsOpponentPiece(_state[target]))
                        AddMove(moveHandler, square, target);
                    break;
                }
    }

    //****************
    //** MOVES **
    //****************

    private void AddMoves(Consumer<Move> moveHandler, int square, byte[][] targets) {
        for (int target : targets[square])
            if (_state[target] == Piece.None)
                AddMove(moveHandler, square, target);
    }

    private void AddMoves(Consumer<Move> moveHandler, int square, byte[][][] targets) {
        for (var axis : targets[square])
            for (int target : axis)
                if (_state[target] == Piece.None)
                    AddMove(moveHandler, square, target);
                else
                    break;
    }

    //****************
    //** KING MOVES **
    //****************

    private void AddWhiteCastlingMoves(Consumer<Move> moveHandler) {
        //Castling is only possible if it's associated CastlingRight flag is set? it get's cleared when either the king or the matching rook move and provide a cheap early out
        if (HasCastlingRight(CastlingRights.WhiteQueenside) && CanCastle(WhiteKingSquare, WhiteQueensideRookSquare, Color.White))
            moveHandler.accept(Move.WhiteCastlingLong);

        if (HasCastlingRight(CastlingRights.WhiteKingside) && CanCastle(WhiteKingSquare, WhiteKingsideRookSquare, Color.White))
            moveHandler.accept(Move.WhiteCastlingShort);
    }


    private void AddBlackCastlingMoves(Consumer<Move> moveHandler) {
        if (HasCastlingRight(CastlingRights.BlackQueenside) && CanCastle(BlackKingSquare, BlackQueensideRookSquare, Color.Black))
            moveHandler.accept(Move.BlackCastlingLong);

        if (HasCastlingRight(CastlingRights.BlackKingside) && CanCastle(BlackKingSquare, BlackKingsideRookSquare, Color.Black))
            moveHandler.accept(Move.BlackCastlingShort);
    }

    private boolean CanCastle(int kingSquare, int rookSquare, Color color) {
        if (_state[kingSquare] != Piece.King.OfColor(color) || _state[rookSquare] != Piece.Rook.OfColor(color))
            throw new IllegalArgumentException("CanCastle shouldn't be called if castling right has been lost!");

        Color enemyColor = Pieces.Flip(color);
        int gap = Math.abs(rookSquare - kingSquare) - 1;
        int dir = Integer.signum(rookSquare - kingSquare);

        // the squares *between* the king and the rook need to be unoccupied
        for (int i = 1; i <= gap; i++)
            if (_state[kingSquare + i * dir] != Piece.None)
                return false;

        //the king must not start, end or pass through a square that is attacked by an enemy piece. (but the rook and the square next to the rook on queenside may be attacked)
        for (int i = 0; i < 3; i++)
            if (IsSquareAttackedBy(kingSquare + i * dir, enemyColor))
                return false;

        return true;
    }

    //*****************
    //** PAWN MOVES ***
    //*****************

    private void AddWhitePawnMoves(Consumer<Move> moveHandler, int square) {
        //if the square above isn't free there are no legal moves
        if (_state[up(square)] != Piece.None)
            return;

        AddWhitePawnMove(moveHandler, square, up(square));

        //START POS? => consider double move
        if (Rank(square) == 1 && _state[Up(square, 2)] == Piece.None)
            AddMove(moveHandler, square, Up(square, 2));
    }

    private void AddBlackPawnMoves(Consumer<Move> moveHandler, int square) {
        //if the square below isn't free there are no legal moves
        if (_state[down(square)] != Piece.None)
            return;

        AddBlackPawnMove(moveHandler, square, down(square));
        //START POS? => consider double move
        if (Rank(square) == 6 && _state[Down(square, 2)] == Piece.None)
            AddMove(moveHandler, square, Down(square, 2));
    }

    private void AddWhitePawnAttacks(Consumer<Move> moveHandler, int square) {
        for (int target : Attacks.WhitePawn[square])
            if (_state[target].IsBlack() || target == _enPassantSquare)
                AddWhitePawnMove(moveHandler, square, target);
    }

    private void AddBlackPawnAttacks(Consumer<Move> moveHandler, int square) {
        for (int target : Attacks.BlackPawn[square])
            if (_state[target].IsWhite() || target == _enPassantSquare)
                AddBlackPawnMove(moveHandler, square, target);
    }

    private void AddBlackPawnMove(Consumer<Move> moveHandler, int from, int to) {
        if (Rank(to) == 0) //Promotion?
        {
            AddPromotion(moveHandler, from, to, Piece.BlackQueen);
            AddPromotion(moveHandler, from, to, Piece.BlackRook);
            AddPromotion(moveHandler, from, to, Piece.BlackBishop);
            AddPromotion(moveHandler, from, to, Piece.BlackKnight);
        } else
            AddMove(moveHandler, from, to);
    }

    private void AddWhitePawnMove(Consumer<Move> moveHandler, int from, int to) {
        if (Rank(to) == 7) //Promotion?
        {
            AddPromotion(moveHandler, from, to, Piece.WhiteQueen);
            AddPromotion(moveHandler, from, to, Piece.WhiteRook);
            AddPromotion(moveHandler, from, to, Piece.WhiteBishop);
            AddPromotion(moveHandler, from, to, Piece.WhiteKnight);
        } else
            AddMove(moveHandler, from, to);
    }

    //**************
    //** Utility ***
    //**************
    private int Rank(int index) {
        return index / 8;
    }

    private int up(int index) {
        return index + 8;
    }

    private int Up(int index, int steps) {
        return index + steps * 8;
    }

    private int down(int index) {
        return index - 8;
    }

    private int Down(int index, int steps) {
        return index - steps * 8;
    }

    private boolean IsActivePiece(Piece piece) {
        return piece.Color() == _sideToMove;
    }

    private boolean IsOpponentPiece(Piece piece) {
        return piece.Color() == Pieces.Flip(_sideToMove);
    }

    private boolean HasCastlingRight(byte flag) {
        return (_castlingRights & flag) == flag;
    }

    private void SetCastlingRights(byte flag, boolean state) {
        if (state)
            _castlingRights |= flag;
        else
            _castlingRights &= (byte) ~flag;
    }

    private void InitZobristHash()
    {
        //Side to move
        _zobristHash = Zobrist.SideToMove(_sideToMove);
        //Pieces
        for (int square = 0; square < 64; square++)
            _zobristHash ^= Zobrist.PieceSquare(_state[square], square);
        //En passant
        _zobristHash ^= Zobrist.Castling(_castlingRights);
        //Castling
        _zobristHash ^= Zobrist.EnPassant(_enPassantSquare);
    }

    @Override
    public String toString() {
        StringBuilder r = new StringBuilder();
        for (int i = 7; i >= 0; i--) {
            for (int j = 0; j < 8; j++)
                r.append(Notation.ToChar(get(i, j)));
            r.append("\n");
        }
        return r.toString();
    }
}