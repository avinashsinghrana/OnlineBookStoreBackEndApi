package com.bridgelabz.bookstore.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.bridgelabz.bookstore.model.BookModel;

@Repository
public interface BookRepository extends JpaRepository<BookModel, Long>  {
	

	@Query(value = "SELECT * from  bookstorebackendapi.book where is_verfied =0", nativeQuery = true)
	List<BookModel> getAllUnverfiedBooks();
	
	@Query(value = "select * from book where is_verfied = 1 order by price asc", nativeQuery = true)
	List<BookModel> sortBookAsc();
	
	@Query(value = "select * from book where is_verfied = 1 order by price desc", nativeQuery = true)

	List<BookModel>  sortBookDesc();
	
	@Query(value="Select * from book where  is_verfied=1",nativeQuery = true)
	List<BookModel>getAllBooks();
	
	@Query(value="Select * from book where  book_id=?1",nativeQuery = true)
	BookModel getBookDetail(Long bookid);

	BookModel findByBookId(Long bookId);

    void deleteByBookId(Long bookId);

//	@Query(value="Select * from book where  book_id=?1 and is_verfied=1",nativeQuery = true)
//	BookModel getBookDetail(Long bookid);

}
