//Copyright (c) 2017. 章钦豪. All rights reserved.
package com.monke.monkeybook.model;

import android.annotation.SuppressLint;

import com.hwangjr.rxbus.RxBus;
import com.monke.monkeybook.bean.BaseChapterBean;
import com.monke.monkeybook.bean.BookContentBean;
import com.monke.monkeybook.bean.BookShelfBean;
import com.monke.monkeybook.bean.ChapterListBean;
import com.monke.monkeybook.bean.SearchBookBean;
import com.monke.monkeybook.dao.ChapterListBeanDao;
import com.monke.monkeybook.dao.DbHelper;
import com.monke.monkeybook.help.BookshelfHelp;
import com.monke.monkeybook.help.RxBusTag;
import com.monke.monkeybook.model.content.DefaultModelImpl;
import com.monke.monkeybook.model.impl.IStationBookModel;
import com.monke.monkeybook.model.impl.IWebBookModel;
import com.monke.monkeybook.model.source.My716;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Scheduler;

public class WebBookModelImpl implements IWebBookModel {

    public static WebBookModelImpl getInstance() {
        return new WebBookModelImpl();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 网络请求并解析书籍信息
     * return BookShelfBean
     */
    @Override
    public Observable<BookShelfBean> getBookInfo(BookShelfBean bookShelfBean) {
        IStationBookModel bookModel = getBookSourceModel(bookShelfBean.getTag());
        if (bookModel != null) {
            return bookModel.getBookInfo(bookShelfBean);
        } else {
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 网络解析图书目录
     * return BookShelfBean
     */
    @Override
    public Observable<BookShelfBean> getChapterList(final BookShelfBean bookShelfBean) {
        IStationBookModel bookModel = getBookSourceModel(bookShelfBean.getTag());
        if (bookModel != null) {
            return bookModel.getChapterList(bookShelfBean)
                    .flatMap((chapterList) -> getChapterList(bookShelfBean, chapterList));
        } else {
            return Observable.error(new Throwable(bookShelfBean.getBookInfoBean().getName() + "没有书源"));
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 章节缓存
     */
    @Override
    public Observable<BookContentBean> getBookContent(final Scheduler scheduler, BaseChapterBean chapterBean, String bookName) {
        IStationBookModel bookModel = getBookSourceModel(chapterBean.getTag());
        if (bookModel != null) {
            return bookModel.getBookContent(scheduler, chapterBean.getDurChapterUrl(), chapterBean.getDurChapterIndex())
                    .flatMap((bookContentBean -> saveContent(bookName, chapterBean, bookContentBean)));
        } else
            return Observable.create(e -> {
                e.onError(new Throwable("没有找到书源"));
                e.onComplete();
            });
    }

    /**
     * 其他站点集合搜索
     */
    @Override
    public Observable<List<SearchBookBean>> searchOtherBook(String content, int page, String tag) {
        //获取所有书源类
        IStationBookModel bookModel = getBookSourceModel(tag);
        if (bookModel != null) {
            return bookModel.searchBook(content, page);
        } else {
            return Observable.create(e -> {
                e.onNext(new ArrayList<>());
                e.onComplete();
            });
        }
    }

    /**
     * 发现页
     */
    @Override
    public Observable<List<SearchBookBean>> findBook(String url, int page, String tag) {
        IStationBookModel bookModel = getBookSourceModel(tag);
        if (bookModel != null) {
            return bookModel.findBook(url, page);
        } else {
            return Observable.create(e -> {
                e.onNext(new ArrayList<>());
                e.onComplete();
            });
        }
    }

    //获取book source class
    private IStationBookModel getBookSourceModel(String tag) {
        switch (tag) {
            case BookShelfBean.LOCAL_TAG:
                return null;
            case My716.TAG:
                return My716.getInstance();
            default:
                return DefaultModelImpl.getInstance(tag);
        }
    }

    private Observable<BookShelfBean> getChapterList(BookShelfBean bookShelfBean, List<ChapterListBean> chapterList) {
        return Observable.create(e -> {
            for (int i = 0; i < chapterList.size(); i++) {
                ChapterListBean chapter = chapterList.get(i);
                chapter.setDurChapterIndex(i);
                chapter.setTag(bookShelfBean.getTag());
                chapter.setNoteUrl(bookShelfBean.getNoteUrl());
            }
            if (bookShelfBean.getChapterListSize() < chapterList.size()) {
                bookShelfBean.setHasUpdate(true);
                bookShelfBean.setFinalRefreshData(System.currentTimeMillis());
                bookShelfBean.getBookInfoBean().setFinalRefreshData(System.currentTimeMillis());
            }
            bookShelfBean.setChapterListSize(chapterList.size());
            bookShelfBean.setDurChapter(Math.min(bookShelfBean.getDurChapter(), bookShelfBean.getChapterListSize() - 1));
            bookShelfBean.getBookInfoBean().setChapterList(chapterList);
            bookShelfBean.upDurChapterName();
            bookShelfBean.upLastChapterName();
            DbHelper.getInstance().getmDaoSession().getChapterListBeanDao().queryBuilder()
                    .where(ChapterListBeanDao.Properties.NoteUrl.eq(bookShelfBean.getNoteUrl()))
                    .buildDelete().executeDeleteWithoutDetachingEntities();
            e.onNext(bookShelfBean);
            e.onComplete();
        });
    }

    @SuppressLint("DefaultLocale")
    private Observable<BookContentBean> saveContent(String bookName, BaseChapterBean chapterBean, BookContentBean bookContentBean) {
        return Observable.create(e -> {
            bookContentBean.setNoteUrl(chapterBean.getNoteUrl());
            if (bookContentBean.getDurChapterContent() == null) {
                e.onError(new Throwable("下载章节出错"));
            } else if (BookshelfHelp.saveChapterInfo(bookName + "-" + chapterBean.getTag(), chapterBean.getDurChapterIndex(),
                    chapterBean.getDurChapterName(), bookContentBean.getDurChapterContent())) {
                RxBus.get().post(RxBusTag.CHAPTER_CHANGE, chapterBean);
                e.onNext(bookContentBean);
            } else {
                e.onError(new Throwable("保存章节出错"));
            }
            e.onComplete();
        });
    }
}